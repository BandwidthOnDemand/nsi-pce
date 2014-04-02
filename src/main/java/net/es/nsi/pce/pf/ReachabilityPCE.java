package net.es.nsi.pce.pf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
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

public class ReachabilityPCE implements PCEModule {

    private ServiceInfoProvider serviceInfoProvider;

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

    protected Optional<Path> findPath(PCEData pceData) {
        Constraints constraints = new Constraints(pceData.getConstraints());

        String sourceStp = getSourceStpOrFail(constraints);
        String destStp = getDestinationStpOrFail(constraints);

        String sourceNetworkId = extractNetworkIdOrFail(sourceStp, Point2Point.SOURCESTP);
        String destNetworkId = extractNetworkIdOrFail(destStp, Point2Point.DESTSTP);

        if (isMyNetwork(sourceNetworkId, pceData) || isMyNetwork(destNetworkId, pceData)) {
           // TODO ...
           return Optional.absent();
        } else {
            return findForwardPath(sourceStp, destStp, pceData);
        }
    }

    //TODO loop detection
    protected Optional<Path> findForwardPath(String sourceStp, String destStp, PCEData pceData) {
        String sourceNetworkId = extractNetworkIdOrFail(sourceStp, Point2Point.SOURCESTP);
        String destNetworkId = extractNetworkIdOrFail(destStp, Point2Point.DESTSTP);

        Optional<Reachability> forwardNsa = findCheapestForwardNsa(sourceNetworkId, destNetworkId, pceData);

        if (forwardNsa.isPresent()) {
            StpType a = new StpType();
            a.setId(sourceStp);
            a.setNetworkId(forwardNsa.get().getNetworkId());
            StpType z = new StpType();
            z.setId(destStp);
            z.setNetworkId(forwardNsa.get().getNetworkId());

            Path path = new Path();
            path.addStpPair(new StpPair(a, z));

            return Optional.of(path);
        }

        return Optional.absent();
    }

    private Optional<Reachability> findCheapestForwardNsa(String sourceNetworkId, String destNetworkId, PCEData pceData) {
        Optional<Reachability> sourceCost = determineCost(sourceNetworkId, pceData.getReachabilityTable());
        Optional<Reachability> destCost = determineCost(destNetworkId, pceData.getReachabilityTable());

        if (sourceCost.isPresent()) {
            if (destCost.isPresent()) {
                return sourceCost.get().getCost() < destCost.get().getCost() ? sourceCost : destCost;
            } else {
                return sourceCost;
            }
        } else {
            return destCost;
        }
    }

    private boolean isMyNetwork(String networkId, PCEData pceData) {
        return pceData.getLocalManagedNetworkIds().contains(networkId);
    }

    private final static Ordering<Entry<String, Map<String, Integer>>> REACHABILITY_TABLE_ORDERING = Ordering.from(new Comparator<Entry<String, Map<String, Integer>>>() {
        @Override
        public int compare(Entry<String, Map<String, Integer>> o1, Entry<String, Map<String, Integer>> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    });

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

    private String extractNetworkIdOrFail(String stpId, String attributeName) {
        Optional<String> topologyId = extractNetworkId(stpId);
        if (topologyId.isPresent()) {
            return topologyId.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.NAMESPACE, attributeName));
    }

    protected Optional<String> extractNetworkId(String stpId) {
        checkNotNull(stpId);

        Iterable<String> parts = Iterables.limit(Splitter.on(":").split(stpId), 6);

        return Iterables.size(parts) == 6 ? Optional.of(Joiner.on(":").join(parts)) : Optional.<String>absent();
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

}
