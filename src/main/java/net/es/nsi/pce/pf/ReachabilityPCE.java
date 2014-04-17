package net.es.nsi.pce.pf;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraints;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
@Component
@Scope("prototype")
public class ReachabilityPCE implements PCEModule {

    private final static Ordering<Entry<String, Map<String, Integer>>> REACHABILITY_TABLE_ORDERING = Ordering.from(new Comparator<Entry<String, Map<String, Integer>>>() {
        @Override
        public int compare(Entry<String, Map<String, Integer>> o1, Entry<String, Map<String, Integer>> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    });

    @Override
    public PCEData apply(PCEData pceData) {
        checkNotNull(pceData.getTrace(), "No trace was provided");
        checkNotNull(pceData.getTopology(), "No topology was provided");

        Constraints constraints = pceData.getAttrConstraints();
        Stp sourceStp = findSourceStp(constraints);
        Stp destStp = findDestinationStp(constraints);

        Optional<Path> path = findPath(sourceStp, destStp, pceData.getTopology(), pceData.getTopology().getReachabilityTable(), pceData.getTrace());

        if (path.isPresent()) {
            assertAllSegmentsHaveAnNsa(path.get());
            addConstraints(path.get(), constraints);
            pceData.setPath(path.get());
        }

        return pceData;
    }

    private void addConstraints(Path path, Constraints constraints) {
        constraints.removeStringAttrConstraint(Point2Point.SOURCESTP);
        constraints.removeStringAttrConstraint(Point2Point.DESTSTP);
        for (PathSegment segment: path.getPathSegments()) {
            segment.setConstraints(constraints);
        }
    }

    private void assertAllSegmentsHaveAnNsa(Path path) {
        boolean invalid = Iterables.any(path.getPathSegments(), new Predicate<PathSegment>() {
            @Override
            public boolean apply(PathSegment segment) {
                return isNullOrEmpty(segment.getNsaId()) || isNullOrEmpty(segment.getCsProviderURL());
            }
        });

        if (invalid) throw new AssertionError("Not all path segments have a nsa id " + path.getPathSegments());
    }

    @VisibleForTesting
    protected Optional<Path> findPath(Stp sourceStp, Stp destStp, NsiTopology topology, Map<String, Map<String, Integer>> reachabilityTable, List<String> connectionTrace) {
        if (isInMyNetwork(sourceStp, topology)) {
            if (isInMyNetwork(destStp, topology)) {
                return onlyLocalPath(sourceStp, destStp, topology);
            } else {
                return findSplitPath(sourceStp, destStp, topology, reachabilityTable, connectionTrace);
            }
        } else if (isInMyNetwork(destStp, topology)) {
            return findSplitPath(destStp, sourceStp, topology, reachabilityTable, connectionTrace);
        } else {
            return findForwardPath(sourceStp, destStp, topology, reachabilityTable, connectionTrace);
        }
    }

    private Stp findSourceStp(Constraints constraints) {
        String sourceStp = getSourceStpOrFail(constraints);
        try {
            return Stp.fromStpId(sourceStp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.NAMESPACE, Point2Point.SOURCESTP));
        }
    }

    private Stp findDestinationStp(Constraints constraints) {
        String destStp = getDestinationStpOrFail(constraints);
        try {
            return Stp.fromStpId(destStp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.NAMESPACE, Point2Point.DESTSTP));
        }
    }

    private Optional<Path> onlyLocalPath(Stp sourceStp, Stp destStp, NsiTopology topology) {
        checkArgument(sourceStp.getNetworkId().equals(destStp.getNetworkId()));

        return Optional.of(new Path(new PathSegment(new StpPair(sourceStp.toStpType(), destStp.toStpType())).withNsa(topology.getLocalNsaId(), topology.getLocalProviderUrl())));
    }

    @VisibleForTesting
    protected Optional<Path> findSplitPath(Stp localStp, Stp remoteStp, NsiTopology topology, Map<String, Map<String, Integer>> reachabilityTable, List<String> connectionTrace) {
        Optional<Reachability> remoteNsa = findPeerWithLowestCostToReachNetwork(remoteStp.getNetworkId(), reachabilityTable);

        checkNotIntroducingLoop(remoteNsa, connectionTrace);

        String remoteNsaId = remoteNsa.get().getNsaId();
        Optional<SdpType> connectingSdp = remoteNsa.isPresent() ?
            findConnectingSdp(localStp.getNetworkId(), remoteNsaId, topology) : Optional.<SdpType>absent();

        if (!connectingSdp.isPresent()) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, Point2Point.NAMESPACE, Point2Point.DESTSTP));
        }

        Stp localIntermediateStp = findStpFromSdp(connectingSdp.get(), localStp.getNetworkId());
        Stp remoteIntermediateStp = findOtherStpFromSdp(connectingSdp.get(), localIntermediateStp);

        StpPair localStpPair = new StpPair(localStp.toStpType(), localIntermediateStp.toStpType());
        StpPair remoteStpPair = new StpPair(remoteIntermediateStp.toStpType(), remoteStp.toStpType());

        PathSegment localSegment = new PathSegment(localStpPair).withNsa(topology.getLocalNsaId(), topology.getLocalProviderUrl());
        PathSegment forwardSegment = new PathSegment(remoteStpPair).withNsa(remoteNsaId, topology.getProviderUrl(remoteNsaId));

        return Optional.of(new Path(localSegment, forwardSegment));
    }

    private Stp findOtherStpFromSdp(SdpType sdp, Stp stp) {
        if (sdp.getDemarcationA().getNetwork().getId().equals(stp.getNetworkId())) {
            return Stp.fromDemarcation(sdp.getDemarcationZ());
        } else {
            return Stp.fromDemarcation(sdp.getDemarcationA());
        }
    }

    private Stp findStpFromSdp(SdpType sdp, String networkId) {
        return sdp.getDemarcationA().getNetwork().getId().equals(networkId) ? Stp.fromDemarcation(sdp.getDemarcationA()) : Stp.fromDemarcation(sdp.getDemarcationZ());
    }

    private Optional<SdpType> findConnectingSdp(String networkId, String nsaId, NsiTopology topology) {
        List<ResourceRefType> remoteNetworks = topology.getNsa(nsaId).getNetwork();
        for (ResourceRefType network : remoteNetworks) {
            Optional<SdpType> sdp = findSdp(networkId, network.getId(), topology.getSdps());
            if (sdp.isPresent()) {
                return sdp;
            }
        }

        return Optional.absent();
    }

    private Optional<SdpType> findSdp(String networkIdA, String networkIdZ, Collection<SdpType> sdps) {
        for (SdpType sdp : sdps) {
            if (sdp.getDemarcationA().getNetwork().getId().equals(networkIdA) && sdp.getDemarcationZ().getNetwork().getId().equals(networkIdZ)) {
                return Optional.of(sdp);
            } else if (sdp.getDemarcationA().getNetwork().getId().equals(networkIdZ) && sdp.getDemarcationZ().getNetwork().getId().equals(networkIdA)) {
                return Optional.of(sdp);
            }
        }
        return Optional.absent();
    }

    @VisibleForTesting
    protected Optional<Path> findForwardPath(final Stp sourceStp, final Stp destStp, final NsiTopology topology, Map<String, Map<String, Integer>> reachabilityTable, List<String> connectionTrace) {
        final Optional<Reachability> forwardNsa = findCheapestForwardNsa(sourceStp, destStp, reachabilityTable);

        checkNotIntroducingLoop(forwardNsa, connectionTrace);

        return forwardNsa.transform(new Function<Reachability, Path>() {
            @Override
            public Path apply(Reachability reachability) {
                String forwardNsaId = forwardNsa.get().getNsaId();
                PathSegment segment = new PathSegment(new StpPair(sourceStp.toStpType(), destStp.toStpType())).withNsa(forwardNsaId, topology.getProviderUrl(forwardNsaId));
                return new Path(segment);
            }
        });
    }

    private void checkNotIntroducingLoop(Optional<Reachability> forwardNsa, List<String> connectionTrace) {
        if (forwardNsa.isPresent() && connectionTrace.contains(forwardNsa.get().getNsaId())) {
            throw new IllegalArgumentException("Loop detected");
        }
    }

    private Optional<Reachability> findCheapestForwardNsa(Stp sourceStp, Stp destStp, Map<String, Map<String, Integer>> reachabilityTable) {
        Optional<Reachability> sourceCost = findPeerWithLowestCostToReachNetwork(sourceStp.getNetworkId(), reachabilityTable);
        Optional<Reachability> destCost = findPeerWithLowestCostToReachNetwork(destStp.getNetworkId(), reachabilityTable);

        if (sourceCost.isPresent()) {
            if (destCost.isPresent()) {
                return sourceCost.get().getCost() <= destCost.get().getCost() ? sourceCost : destCost;
            } else {
                return sourceCost;
            }
        } else {
            return destCost;
        }
    }

    private boolean isInMyNetwork(Stp stp, NsiTopology topology) {
        return topology.getLocalNetworks().contains(stp.getNetworkId());
    }

    @VisibleForTesting
    protected Optional<Reachability> findPeerWithLowestCostToReachNetwork(String networkId, Map<String, Map<String, Integer>> reachabilityTable) {
        Optional<Reachability> reachability = Optional.absent();

        for (Entry<String, Map<String, Integer>> nsaCosts : REACHABILITY_TABLE_ORDERING.sortedCopy(reachabilityTable.entrySet())) {
            if (nsaCosts.getValue().containsKey(networkId)) {
                Integer nsaCost = nsaCosts.getValue().get(networkId);
                if (!reachability.isPresent() || reachability.get().getCost() > nsaCost) {
                    reachability = Optional.of(new Reachability(nsaCost, nsaCosts.getKey()));
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

    public static class Reachability {
        private final Integer cost;
        private final String nsaId;

        public Reachability(Integer cost, String nsaId) {
            this.cost = cost;
            this.nsaId = nsaId;
        }
        public Integer getCost() {
            return cost;
        }
        public String getNsaId() {
            return nsaId;
        }
    }

    public static class Stp {
        private static final Splitter STP_SPLITTER = Splitter.on(":");
        private static final Joiner STP_JOINER = Joiner.on(":");

        private final String id;
        private final String networkId;

        public static Stp fromDemarcation(DemarcationType demarcation) {
            checkNotNull(demarcation);
            return fromStpId(demarcation.getStp().getId());
        }

        public static Stp fromStpId(String id) {
            checkNotNull(id);

            Optional<String> networkId = extractNetworkId(id);
            if (!networkId.isPresent()) {
                throw new IllegalArgumentException(String.format("Could not extract network id from '%s'", id));
            }

            return new Stp(id, networkId.get());
        }

        private Stp(String id, String networkId) {
            this.id = id;
            this.networkId = networkId;
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
            StpType stpType = new StpType();
            stpType.setId(id);
            stpType.setNetworkId(networkId);
            return stpType;
        }
    }
}
