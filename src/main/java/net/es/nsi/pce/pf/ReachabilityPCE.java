package net.es.nsi.pce.pf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraints;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.StpType;

/**
 * This PCE module calculates the path based on reachability information.
 *
 * The paths (data plane) always follows the control plane connections.
 *
 * If one of the source or destination STP is in the network the aggregator manages then
 * the request is split up. The remaining path is send to a peer.
 *
 * Otherwise the request needs to be forwarded. The target nsa is determined by the
 * reachability information. The request is sent to the nsa that can reach the source or
 * destination topology at the lowest cost.
 *
 * Note: The target nsa is determined in the {@link Point2Point#resolvePath(String, Path)} by the network id on the StpType in the StpPair.
 * Because we are forwarding to a nsa we need to get a network id managed by that nsa. So the StpType contains a id (stpId) that doesn't match the networkId.
 * The networkId is queried from the {@link ServiceInfoProvider} by nsa id.
 */
public class ReachabilityPCE implements PCEModule {

    private final static Ordering<Entry<String, Map<String, Integer>>> REACHABILITY_TABLE_ORDERING = Ordering.from(new Comparator<Entry<String, Map<String, Integer>>>() {
        @Override
        public int compare(Entry<String, Map<String, Integer>> o1, Entry<String, Map<String, Integer>> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    });

    private final ServiceInfoProvider serviceInfoProvider;

    public ReachabilityPCE(ServiceInfoProvider serviceInfoProvider) {
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @Override
    public PCEData apply(PCEData pceData) {
        Optional<Path> path = findPath(pceData);

        if (path.isPresent()) {
            pceData.setPath(path.get());
        }

        return pceData;
    }

    @VisibleForTesting
    protected Optional<Path> findPath(PCEData pceData) {
        Constraints constraints = pceData.getAttrConstraints();
        Stp sourceStp = findSourceStp(constraints);
        Stp destStp = findDestinationStp(constraints);

        if (isInMyNetwork(sourceStp, pceData)) {
            if (isInMyNetwork(destStp, pceData)) {
                return onlyLocalPath(sourceStp, destStp);
            } else {
                return findSplitPath();
            }
        } else if (isInMyNetwork(destStp, pceData)) {
            return findSplitPath();
        } else {
            return findForwardPath(sourceStp, destStp, pceData.getReachabilityTable());
        }
    }

    private Stp findSourceStp(Constraints constraints) {
        String sourceStp = getSourceStpOrFail(constraints);
        try {
            return new Stp(sourceStp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.NAMESPACE, Point2Point.SOURCESTP));
        }
    }

    private Stp findDestinationStp(Constraints constraints) {
        String destStp = getDestinationStpOrFail(constraints);
        try {
            return new Stp(destStp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.NAMESPACE, Point2Point.DESTSTP));
        }
    }

    private Optional<Path> onlyLocalPath(Stp sourceStp, Stp destStp) {
        Preconditions.checkArgument(sourceStp.getNetworkId().equals(destStp.getNetworkId()));

        return Optional.of(new Path(new StpPair(sourceStp.toStpType(), destStp.toStpType())));
    }

    protected Optional<Path> findSplitPath() {
        return Optional.absent();
    }

    //TODO loop detection
    @VisibleForTesting
    protected Optional<Path> findForwardPath(final Stp sourceStp, final Stp destStp, Map<String, Map<String, Integer>> reachabilityTable) {
        final Optional<Reachability> forwardNsa = findCheapestForwardNsa(sourceStp, destStp, reachabilityTable);

        return forwardNsa.transform(new Function<Reachability, Path>() {
            @Override
            public Path apply(Reachability reachability) {
                StpType a = createStpType(sourceStp.getId(), forwardNsa.get().getNetworkId());
                StpType z = createStpType(destStp.getId(), forwardNsa.get().getNetworkId());

                return new Path(new StpPair(a, z));
            }
        });
    }

    private Optional<Reachability> findCheapestForwardNsa(Stp sourceStp, Stp destStp, Map<String, Map<String, Integer>> reachabilityTable) {
        Optional<Reachability> sourceCost = determineCost(sourceStp.getNetworkId(), reachabilityTable);
        Optional<Reachability> destCost = determineCost(destStp.getNetworkId(), reachabilityTable);

        if (sourceCost.isPresent()) {
            if (destCost.isPresent()) {
                // TODO when costs are equal always return source expect when source is in connection trace
                return sourceCost.get().getCost() <= destCost.get().getCost() ? sourceCost : destCost;
            } else {
                return sourceCost;
            }
        } else {
            return destCost;
        }
    }

    private boolean isInMyNetwork(Stp stp, PCEData pceData) {
        return pceData.getLocalManagedNetworkIds().contains(stp.getNetworkId());
    }

    @VisibleForTesting
    protected Optional<Reachability> determineCost(String networkId, Map<String, Map<String, Integer>> reachabilityTable) {
        Optional<Reachability> reachability = Optional.absent();

        for (Entry<String, Map<String, Integer>> nsaCosts : REACHABILITY_TABLE_ORDERING.sortedCopy(reachabilityTable.entrySet())) {
            if (nsaCosts.getValue().containsKey(networkId)) {
                Integer nsaCost = nsaCosts.getValue().get(networkId);
                if (!reachability.isPresent() || reachability.get().getCost() > nsaCost) {
                    ServiceInfo serviceInfo = serviceInfoProvider.byNsaId(nsaCosts.getKey());
                    reachability = Optional.of(new Reachability(nsaCost, serviceInfo.getNetworkId()));
                }
            }
        }

        return reachability;
    }

    private String getSourceStpOrFail(Constraints constraints) {
        return getStringValue(Point2Point.SOURCESTP, constraints);
    }

    private String getDestinationStpOrFail(Constraints constraints) {
        return getStringValue(Point2Point.DESTSTP, constraints);
    }

    private String getStringValue(String attributeName, Constraints constraints) {
        Optional<String> value = getValue(constraints.getStringAttrConstraint(attributeName));

        if (value.isPresent()) {
            return value.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2Point.NAMESPACE, attributeName));
    }

    private Optional<String> getValue(StringAttrConstraint constraint) {
        if (constraint == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(Strings.emptyToNull(constraint.getValue()));
    }

    private static StpType createStpType(String stp, String networkId) {
        StpType stpType = new StpType();
        stpType.setId(stp);
        stpType.setNetworkId(networkId);

        return stpType;
    }

    public static class Reachability {
        private final Integer cost;
        private final String networkId;

        public Reachability(Integer cost, String networkId) {
            this.cost = cost;
            this.networkId = networkId;
        }
        public Integer getCost() {
            return cost;
        }
        public String getNetworkId() {
            return networkId;
        }
    }

    public static class Stp {
        private static final Splitter STP_SPLITTER = Splitter.on(":");
        private static final Joiner STP_JOINER = Joiner.on(":");

        private final String id;
        private final String networkId;

        public Stp(String id) {
            checkNotNull(id);

            Optional<String> networkIdOption = extractNetworkId(id);
            if (!networkIdOption.isPresent()) {
                throw new IllegalArgumentException(String.format("Could not extract network id from '%s'", id));
            }

            this.id = id;
            this.networkId = networkIdOption.get();
        }

        protected static Optional<String> extractNetworkId(String stpId) {
            Iterable<String> parts = Iterables.limit(STP_SPLITTER.split(stpId), 6);
            return Iterables.size(parts) == 6 ? Optional.of(STP_JOINER.join(parts)) : Optional.<String>absent();
        }

        public String getId() {
            return id;
        }
        public String getNetworkId() {
            return networkId;
        }

        public StpType toStpType() {
            return ReachabilityPCE.createStpType(id, networkId);
        }
    }

}
