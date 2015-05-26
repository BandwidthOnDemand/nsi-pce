package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SortedSparseMultigraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.path.services.Service;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main path computation class using Dijkstra's shortest path on an NSI
 * topology model.
 *
 * @author hacksaw
 */
public class DijkstraPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final net.es.nsi.pce.path.jaxb.ObjectFactory factory = new net.es.nsi.pce.path.jaxb.ObjectFactory();

    /**
     * This path computation module supports pathfinding for the P2PS service
     * specification restricted to bidirectional symmetricPath services with
     * two stpId specified in the request.
     *
     * This module does not currently use the following parameters:
     *    - startTime
     *    - endTime
     *    - capacity
     *    - ero
     * @param pceData
     * @return
     */
    @Override
    public PCEData apply(PCEData pceData) {
        checkNotNull(pceData.getTopology(), "DijkstraPCE: No topology was provided");

        // Get constraints this PCE module supports.
        AttrConstraints constraints = pceData.getAttrConstraints();

        // We currently implement P2PS policies in the PCE.  Reject any path
        // finding requests for services we do not understand.
        StringAttrConstraint serviceType = constraints.getStringAttrConstraint(PCEConstraints.SERVICETYPE);
        List<Service> serviceByType = Service.getServiceByType(serviceType.getValue());
        if (!serviceByType.contains(Service.P2PS)) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNSUPPORTED_PARAMETER, Point2PointTypes.P2PS, serviceType.getAttrName(), serviceType.getValue()));
        }

        // Generic reservation information are in string constraint attributes,
        // but the P2PS specific constraints are in the P2PS P2PServiceBaseType.
        P2PServiceBaseType p2p = PfUtils.getP2PServiceBaseTypeOrFail(constraints);

        // Determine directionality of service request, default to bidirectional if not present.
        DirectionalityType directionality = PfUtils.getDirectionality(p2p);

        // Determine path symmetry.
        boolean symmetricPath = PfUtils.getSymmetricPath(p2p);

        // Get source stpId.
        String sourceStp = PfUtils.getSourceStpOrFail(p2p);

        // Get destination stpId.
        String destStp = PfUtils.getDestinationStpOrFail(p2p);

        // Parse the source STP to make sure it is valid.
        SimpleStp srcStpId;
        try {
            srcStpId = new SimpleStp(sourceStp);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), sourceStp));
        }

        // Parse the destination STP to make sure it is valid.
        SimpleStp dstStpId;
        try {
            dstStpId = new SimpleStp(destStp);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), destStp));
        }

        // Get the topology model used for routing.
        NsiTopology nsiTopology = pceData.getTopology();

        log.debug("******* localId " + nsiTopology.getLocalNsaId());
        for (String networkId : nsiTopology.getNetworkIds()) {
            log.debug("******* networkId " + networkId);
        }

        // Look up the STP within our model matching the request.
        StpType srcStp = nsiTopology.getStp(srcStpId.getStpId());
        StpType dstStp = nsiTopology.getStp(dstStpId.getStpId());

        if (srcStp != null) {
            validateDirectionality(srcStp, directionality);
        }

        if (dstStp != null) {
            validateDirectionality(dstStp, directionality);
        }

        List<GraphEdge> path = getPath(srcStp, dstStp, pceData, nsiTopology);

        // Check to see if there is a valid path.
        if (path == null || path.isEmpty()) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "No path found using provided criteria");
            throw new RuntimeException(error);
        }

        List<StpPair> segments = pullIndividualSegmentsOut(path, nsiTopology);
        for (int i = 0; i < segments.size(); i++) {
            StpPair pair = segments.get(i);

            log.debug("Pair: " + pair.getA().getId() + " -- " + pair.getZ().getId());

            // Copy the applicable attribute constraints into this path.
            AttrConstraints cons = new AttrConstraints(pceData.getConstraints());
            constraints.removeObjectAttrConstraint(Point2PointTypes.P2PS);

            // Create a new P2PS constraint for this segment.
            P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
            p2ps.setCapacity(p2p.getCapacity());
            p2ps.setDirectionality(directionality);
            p2ps.setEro(p2p.getEro());
            p2ps.setSymmetricPath(symmetricPath);
            p2ps.setSourceSTP(pair.getA().getId());
            p2ps.setDestSTP(pair.getZ().getId());
            ObjectAttrConstraint p2psObject = new ObjectAttrConstraint();
            p2psObject.setAttrName(Point2PointTypes.P2PS);
            p2psObject.setValue(p2ps);
            cons.add(p2psObject);

            PathSegment pathSegment = new PathSegment(pair);
            pathSegment.setConstraints(cons);
            pceData.getPath().getPathSegments().add(i, pathSegment);
        }

        return pceData;
    }

    private void validateDirectionality(StpType stp, DirectionalityType directionality) throws IllegalArgumentException {
        // Verify the specified STP are of the correct type for the request.
        if (directionality == DirectionalityType.UNIDIRECTIONAL) {
             if (stp.getType() != StpDirectionalityType.INBOUND &&
                     stp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stp.getId()));
            }
        }
        else {
            if (stp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stp.getId()));
            }
        }
    }

    private List<GraphEdge> getPath(StpType srcStp, StpType dstStp, PCEData pceData, NsiTopology nsiTopology) throws IllegalArgumentException, RuntimeException {
        // We build a graph consisting of source STP, destination STP, and all
        // service domains as verticies.  The SDP are added as edges between
        // service domains.  A special edge is added between the edge STP and
        // the first service domain to allow for path finding between the source
        // and destination STP.  We will need to remove any SDP that are
        // associated with the source or destination STP.
        Map<String, GraphVertex> verticies = new HashMap<>();
        Graph<GraphVertex, GraphEdge> graph = new SortedSparseMultigraph<>();
        Transformer<GraphEdge, Number> trans = new EdgeTrasnsformer(pceData);

        // Add the source and destination STP as verticies.
        StpVertex srcVertex = new StpVertex(srcStp.getId(), srcStp);
        graph.addVertex(srcVertex);
        verticies.put(srcVertex.getId(), srcVertex);

        StpVertex dstVertex = new StpVertex(dstStp.getId(), dstStp);
        graph.addVertex(dstVertex);
        verticies.put(dstVertex.getId(), dstVertex);

        // Add ServiceDomains as verticies.
        for (ServiceDomainType serviceDomain : nsiTopology.getServiceDomains()) {
            ServiceDomainVertex vertex = new ServiceDomainVertex(serviceDomain.getId(), serviceDomain);
            graph.addVertex(vertex);
            verticies.put(vertex.getId(), vertex);
        }

        // The source and destination STP need to belong to a service domain
        // otherwise a path will not be found.
        if (srcStp.getServiceDomain() == null) {
            log.error("Missing ServiceDomain for source sdpId=" + srcStp.getId());
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "Missing ServiceDomain for source sdpId=" + srcStp.getId());
            throw new RuntimeException(error);
        }
        else if (dstStp.getServiceDomain() == null) {
            log.error("Missing ServiceDomain for destination sdpId=" + dstStp.getId());
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "Missing ServiceDomain for destination sdpId=" + dstStp.getId());
            throw new RuntimeException(error);
        }

        ServiceDomainType sourceServiceDomain = nsiTopology.getServiceDomain(srcStp.getServiceDomain().getId());
        ServiceDomainType destinationServiceDomain = nsiTopology.getServiceDomain(dstStp.getServiceDomain().getId());

        // Connect the source and destination STP verticies to their associated
        // service domains.
        StpEdge sourceEdge = new StpEdge(srcStp.getId(), srcStp, sourceServiceDomain);
        graph.addEdge(sourceEdge, srcVertex, verticies.get(sourceServiceDomain.getId()));
        StpEdge destinationEdge = new StpEdge(dstStp.getId(), dstStp, destinationServiceDomain);
        graph.addEdge(destinationEdge, dstVertex, verticies.get(destinationServiceDomain.getId()));

        // Get the source and destination SDP and exclude any related SDP to
        // avoid loops.  We expect the STP to be an ingress on a domain and
        // not an egress.
        Set<String> exclusionSdp = new HashSet<>();

        Optional<ResourceRefType> srcSdp = Optional.fromNullable(srcStp.getSdp());
        Optional<ResourceRefType> dstSdp = Optional.fromNullable(dstStp.getSdp());

        if (srcSdp.isPresent()) {
            exclusionSdp.add(srcSdp.get().getId());
        }

        Optional<Map<String, StpType>> srcBundle = Optional.fromNullable(nsiTopology.getStpBundle(srcStp.getId()));
        if (srcBundle.isPresent()) {
            for (StpType stp : srcBundle.get().values()) {
                Optional<ResourceRefType> sdpRef = Optional.fromNullable(stp.getSdp());
                if (sdpRef.isPresent()) {
                    exclusionSdp.add(sdpRef.get().getId());
                }
            }
        }

        if (dstSdp.isPresent()) {
            exclusionSdp.add(dstSdp.get().getId());
        }

        Optional<Map<String, StpType>> dstBundle = Optional.fromNullable(nsiTopology.getStpBundle(dstStp.getId()));
        if (dstBundle.isPresent()) {
            for (StpType stp : dstBundle.get().values()) {
                Optional<ResourceRefType> sdpRef = Optional.fromNullable(stp.getSdp());
                if (sdpRef.isPresent()) {
                    exclusionSdp.add(sdpRef.get().getId());
                }
            }
        }

        // We only do bidirectional path finding at this point so add the
        // bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                Optional<ResourceRefType> aServiceDomainRef = Optional.fromNullable(sdp.getDemarcationA().getServiceDomain());
                Optional<ResourceRefType> zServiceDomainRef = Optional.fromNullable(sdp.getDemarcationZ().getServiceDomain());

                if (!aServiceDomainRef.isPresent()) {
                    log.error("Missing service domain for demarcationA sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationA().getStp().getId());
                }
                else if (!zServiceDomainRef.isPresent()) {
                    log.error("Missing service domain for demarcationZ sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationZ().getStp().getId());
                }
                else if (exclusionSdp.contains(sdp.getId())) {
                    log.debug("Omitting SDP sdpId=" + sdp.getId() + ", due to source stpId=" + srcStp.getId() + " or destination stpId=" + dstStp.getId());
                }
                else {
                    SdpEdge sdpEdge = new SdpEdge(sdp.getId(), sdp);
                    graph.addEdge(sdpEdge, verticies.get(aServiceDomainRef.get().getId()), verticies.get(zServiceDomainRef.get().getId()));
                }
            }
        }

        DijkstraShortestPath<GraphVertex, GraphEdge> alg = new DijkstraShortestPath<>(graph, trans, true);

        List<GraphEdge> path;
        try {
            path = alg.getPath(srcVertex, dstVertex);
        } catch (IllegalArgumentException ex) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, ex.getMessage());
            log.error(error, ex);
            throw new IllegalArgumentException(error);
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");

        return path;
    }

    protected List<StpPair> pullIndividualSegmentsOut(List<GraphEdge> path, NsiTopology nsiTopology) {
        List<StpPair> segments = new ArrayList<>();

        Optional<StpType> start = Optional.absent();
        for (GraphEdge edge: path) {
            log.debug("--- Edge: " + edge.getId());

            if (edge instanceof StpEdge) {
                StpEdge stpEdge = (StpEdge) edge;

                if (!start.isPresent()) {
                    // Source STP in the path.
                    start = Optional.of(stpEdge.getStp());
                }
                else {
                    // Destimnation STP in the path.
                    segments.add(new StpPair(start.get(), stpEdge.getStp()));
                }
            }
            else if (edge instanceof SdpEdge) {
                SdpEdge sdpEdge = (SdpEdge) edge;

                // SDP along the path.
                StpType stpA = nsiTopology.getStp(sdpEdge.getSdp().getDemarcationA().getStp().getId());
                StpType stpZ = nsiTopology.getStp(sdpEdge.getSdp().getDemarcationZ().getStp().getId());

                StpPair pathPair;
                if (start.get().getNetworkId().equalsIgnoreCase(stpA.getNetworkId())) {
                    pathPair = new StpPair(start.get(), stpA);
                    start = Optional.of(stpZ);
                }
                else {
                    pathPair = new StpPair(start.get(), stpZ);
                    start = Optional.of(stpA);
                }
                segments.add(pathPair);
            }
        }

        return segments;
    }
}
