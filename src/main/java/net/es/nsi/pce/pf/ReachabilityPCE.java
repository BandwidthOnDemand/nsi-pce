package net.es.nsi.pce.pf;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;

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

    private static final Logger logger = LoggerFactory.getLogger(ReachabilityPCE.class);

    private final static Ordering<Entry<String, Map<String, Integer>>> REACHABILITY_TABLE_ORDERING = Ordering.from(new Comparator<Entry<String, Map<String, Integer>>>() {
        @Override
        public int compare(Entry<String, Map<String, Integer>> o1, Entry<String, Map<String, Integer>> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    });
    private Random random = new Random();

    // TODO make this configurable at startup
    private final boolean forceIdenticalVlans = true;

    @Override
    public PCEData apply(PCEData pceData) {
        checkNotNull(pceData.getTrace(), "No trace was provided");
        checkNotNull(pceData.getTopology(), "No topology was provided");

        AttrConstraints constraints = pceData.getAttrConstraints();
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

    private void addConstraints(Path path, AttrConstraints constraints) {
        constraints.removeStringAttrConstraint(Point2PointTypes.SOURCESTP);
        constraints.removeStringAttrConstraint(Point2PointTypes.DESTSTP);
        for (PathSegment segment: path.getPathSegments()) {
            segment.setConstraints(new AttrConstraints(constraints));
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
                return findLocalPath(sourceStp, destStp, topology);
            } else {
                return findSplitPath(sourceStp, destStp, topology, reachabilityTable, connectionTrace);
            }
        } else if (isInMyNetwork(destStp, topology)) {
            return findSplitPath(destStp, sourceStp, topology, reachabilityTable, connectionTrace);
        } else {
            return findForwardPath(sourceStp, destStp, topology, reachabilityTable, connectionTrace);
        }
    }

    private Stp findSourceStp(AttrConstraints constraints) {
        String sourceStp = getSourceStpOrFail(constraints);
        try {
            return Stp.fromStpId(sourceStp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), sourceStp));
        }
    }

    private Stp findDestinationStp(AttrConstraints constraints) {
        String destStp = getDestinationStpOrFail(constraints);
        try {
            return Stp.fromStpId(destStp);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), destStp));
        }
    }

    private Optional<Path> findLocalPath(final Stp sourceStp, final Stp destStp, final NsiTopology topology) {
        checkArgument(sourceStp.getNetworkId().equals(destStp.getNetworkId()));

        logger.debug("Trying to find a only local path");

        Optional<String> localProviderUrl = topology.getLocalProviderUrl();
        if (localProviderUrl.isPresent()) {
            return Optional.of(new Path(new PathSegment(new StpPair(sourceStp.toStpType(), destStp.toStpType())).withNsa(topology.getLocalNsaId(), localProviderUrl.get())));
        } else {
            logger.warn("Could not find local provider url (nsaId: {})", topology.getLocalNsaId());
            return Optional.absent();
        }
    }

    @VisibleForTesting
    protected Optional<Path> findSplitPath(Stp localStp, Stp remoteStp, NsiTopology topology, Map<String, Map<String, Integer>> reachabilityTable, List<String> connectionTrace) {
        logger.debug("Trying to find a split (local and remote part) path");

        Optional<Reachability> remoteNsa = findPeerWithLowestCostToReachNetwork(remoteStp.getNetworkId(), topology.getNsaMap(), reachabilityTable);
        logger.debug("found lowest cost peer: {}", remoteNsa);

        checkNotIntroducingLoop(remoteNsa, connectionTrace);

        String remoteNsaId = remoteNsa.get().getNsaId();
        Optional<SdpType> connectingSdp = remoteNsa.isPresent() ?
            findConnectingSdp(localStp.getNetworkId(), remoteNsaId, topology, localStp.getVlan()) : Optional.<SdpType>absent();

        if (!connectingSdp.isPresent()) {
            logger.debug("No connecting sdp found for remoteNsaId {}", remoteNsaId);
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), remoteStp.getId()));
        }

        Stp localIntermediateStp = findStpFromSdp(connectingSdp.get(), localStp.getNetworkId());
        logger.debug("found local intermediate stp {}", localIntermediateStp.getId());

        Stp remoteIntermediateStp = findOtherStpFromSdp(connectingSdp.get(), localIntermediateStp);
        logger.debug("found remote intermediate stp {}", remoteIntermediateStp.getId());

        StpPair localStpPair = new StpPair(localStp.toStpType(), localIntermediateStp.toStpType());
        StpPair remoteStpPair = new StpPair(remoteIntermediateStp.toStpType(), remoteStp.toStpType());

        Optional<String> localProviderUrl = topology.getLocalProviderUrl();
        Optional<String> remoteProviderUrl = topology.getProviderUrl(remoteNsaId);

        if (localProviderUrl.isPresent() && remoteProviderUrl.isPresent()) {
            PathSegment localSegment = new PathSegment(localStpPair).withNsa(topology.getLocalNsaId(), localProviderUrl.get());
            PathSegment forwardSegment = new PathSegment(remoteStpPair).withNsa(remoteNsaId, remoteProviderUrl.get());

            return Optional.of(new Path(localSegment, forwardSegment));
        } else {
            logger.warn("Could not find provider url local: {}, remote: {}", localProviderUrl, remoteProviderUrl);
            return Optional.absent();
        }
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

    private Optional<SdpType> findConnectingSdp(String networkId, String nsaId, NsiTopology topology, Optional<Integer> vlan) {
        List<ResourceRefType> remoteNetworks = topology.getNsa(nsaId).getNetwork();
        for (ResourceRefType network : remoteNetworks) {
            Optional<SdpType> sdp = findSdp(networkId, network.getId(), topology.getSdps(), vlan);
            if (sdp.isPresent()) {
                return sdp;
            }
        }

        return Optional.absent();
    }

    private Optional<SdpType> findSdp(String networkIdA, String networkIdZ, Collection<SdpType> sdps, final Optional<Integer> vlan) {
        List<SdpType> candidates = new ArrayList<>();

        for (SdpType sdp : sdps) {
            if (!sdp.getType().equals(SdpDirectionalityType.BIDIRECTIONAL)) {
                // we can only use bidirectional ports
                continue;
            }
            if (sdp.getDemarcationA().getNetwork().getId().equals(networkIdA) && sdp.getDemarcationZ().getNetwork().getId().equals(networkIdZ)) {
                candidates.add(sdp);
            } else if (sdp.getDemarcationA().getNetwork().getId().equals(networkIdZ) && sdp.getDemarcationZ().getNetwork().getId().equals(networkIdA)) {
                candidates.add(sdp);
            }
        }
        if (candidates.size() == 0) {
            return Optional.absent();
        }
        else if (forceIdenticalVlans && vlan.isPresent()) {
            logger.debug("identical vlans enforced, and vlan specified, asessing {} candidates", candidates.size());
            // underlying NMS probably doesn't support Vlan switching,
            // we must find a path that has the same vlan value in each segment's endpoints

            final String needle = "vlan=" + vlan.get();
            for (SdpType candidate: candidates) {
                if (candidate.getDemarcationA().getStp().getId().contains(needle)
                      && candidate.getDemarcationZ().getStp().getId().contains(needle)) {
                    return Optional.of(candidate);
                }
            }
            logger.warn("no matching vlan found between networks {} and {}", networkIdA, networkIdZ);
            return Optional.absent();
        }
        else {
            // a different sdp instance exists for each matching vlan value. By picking a random one from the candidates
            // each time, we diminish the chances of us trying to use a vlan that is already in use
            // TODO this /will/ fail at times. Solution is to make safnari (the caller) state-aware and let it supply hints
            int randomNum = random.nextInt(candidates.size());
            return Optional.of(candidates.get(randomNum));
        }
    }

    @VisibleForTesting
    protected Optional<Path> findForwardPath(final Stp sourceStp, final Stp destStp, final NsiTopology topology, Map<String, Map<String, Integer>> reachabilityTable, List<String> connectionTrace) {
        logger.debug("Trying to find a forward path");

        final Optional<Reachability> forwardNsa = findCheapestForwardNsa(sourceStp, destStp, topology.getNsaMap(), reachabilityTable);

        checkNotIntroducingLoop(forwardNsa, connectionTrace);

        if (!forwardNsa.isPresent()) {
            return Optional.absent();
        }

        final String forwardNsaId = forwardNsa.get().getNsaId();

        Optional<String> providerUrl = topology.getProviderUrl(forwardNsaId);

        if (!providerUrl.isPresent()) {
            logger.warn("Could not find provider url for forward nsa {}", forwardNsaId);
            return Optional.absent();
        }

        PathSegment segment = new PathSegment(new StpPair(sourceStp.toStpType(), destStp.toStpType())).withNsa(forwardNsaId, providerUrl.get());
        return Optional.of(new Path(segment));
    }

    private void checkNotIntroducingLoop(Optional<Reachability> forwardNsa, List<String> connectionTrace) {
        if (forwardNsa.isPresent() && connectionTrace.contains(forwardNsa.get().getNsaId())) {
            throw new IllegalArgumentException("Loop detected");
        }
    }

    private Optional<Reachability> findCheapestForwardNsa(Stp sourceStp, Stp destStp, Map<String, NsaType> directPeerNsas, Map<String, Map<String, Integer>> reachabilityTable) {
        Optional<Reachability> sourceCost = findPeerWithLowestCostToReachNetwork(sourceStp.getNetworkId(), directPeerNsas, reachabilityTable);
        Optional<Reachability> destCost = findPeerWithLowestCostToReachNetwork(destStp.getNetworkId(), directPeerNsas, reachabilityTable);

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
    protected Optional<Reachability> findPeerWithLowestCostToReachNetwork(final String networkId, final Map<String, NsaType> directPeerNsas, final Map<String, Map<String, Integer>> reachabilityTable) {
        Optional<Reachability> reachability = Optional.absent();

        // let direct peers take precedence when its one of their networks that we want to route to
        for (final String nsaId: directPeerNsas.keySet()) {
            NsaType nsaType = directPeerNsas.get(nsaId);
            for (ResourceRefType resourceRefType: nsaType.getNetwork()) {
                if (resourceRefType.getId().equals(networkId)) {
                    return Optional.of(new Reachability(0, nsaId));
                }
            }
        }

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

    private String getSourceStpOrFail(AttrConstraints constraints) {
        return getStringValue(Point2PointTypes.SOURCESTP, constraints);
    }

    private String getDestinationStpOrFail(AttrConstraints constraints) {
        return getStringValue(Point2PointTypes.DESTSTP, constraints);
    }

    private String getStringValue(String attributeName, AttrConstraints constraints) {
        Optional<String> value = getValue(constraints.getStringAttrConstraint(attributeName));

        if (value.isPresent()) {
            return value.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.P2PS, attributeName, null));
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

        @Override
        public String toString() {
            return "Reachability{" +
                    "cost=" + cost +
                    ", nsaId='" + nsaId + '\'' +
                    '}';
        }
    }

    public static class Stp {
        private static final Splitter STP_SPLITTER = Splitter.on(":");
        private static final Joiner STP_JOINER = Joiner.on(":");
        private static Pattern VLAN_PATTERN = Pattern.compile("[?&]vlan=([^&]+)$");

        private final String id;
        private final String networkId;
        private final Optional<Integer> vlan;


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

            Optional<Integer> vlan = extractVlan(id);
            return new Stp(id, networkId.get(), vlan);
        }

        private Stp(String id, String networkId, Optional<Integer> vlan) {
            this.id = id;
            this.networkId = networkId;
            this.vlan = vlan;
        }

        protected static Optional<String> extractNetworkId(String stpId) {
            Iterable<String> parts = Iterables.limit(STP_SPLITTER.split(stpId), 6);
            return Iterables.size(parts) == 6 ? Optional.of(STP_JOINER.join(parts)) : Optional.<String>absent();
        }

        private static Optional<Integer> extractVlan(final String id) {
            Matcher matcher = VLAN_PATTERN.matcher(id);
            if (matcher.find()) {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            }
            return Optional.absent();
        }

        public String getId() {
            return id;
        }
        public String getNetworkId() {
            return networkId;
        }

        public Optional<Integer> getVlan() {
            return vlan;
        }

        public StpType toStpType() {
            StpType stpType = new StpType();
            stpType.setId(id);
            stpType.setNetworkId(networkId);
            return stpType;
        }

        @Override
        public String toString() {
            return "Stp{" +
                    "id='" + id + '\'' +
                    ", networkId='" + networkId + '\'' +
                    ", vlan=" + vlan +
                    '}';
        }
    }
}
