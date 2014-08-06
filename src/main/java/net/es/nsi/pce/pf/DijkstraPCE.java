package net.es.nsi.pce.pf;

import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import java.util.HashMap;
import java.util.Map;
import com.google.common.base.Optional;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.pf.api.NsiError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraints;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import org.apache.commons.collections15.Transformer;

/**
 * Main path computation class using Dijkstra's shortest path on an NSI
 * topology model.
 *
 * @author hacksaw
 */
public class DijkstraPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, DijkstraVertex> verticies = new HashMap<>();

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
     */
    @Override
    public PCEData apply(PCEData pceData) {

        // Parse out the constraints this PCE module supports.
        Constraints constraints = new Constraints(pceData.getConstraints());

        // Determine directionality of service request, default to bidirectional if not present.
        DirectionalityType directionality = DirectionalityType.BIDIRECTIONAL;
        StringAttrConstraint directionalityConstraint = constraints.getStringAttrConstraint(Point2PointTypes.DIRECTIONALITY);
        if (directionalityConstraint != null) {
            directionality = DirectionalityType.valueOf(directionalityConstraint.getValue());
        }

        // Determine path symmetry.
        boolean symmetricPath = true;
        BooleanAttrConstraint symmetricPathConstraint = constraints.getBooleanAttrConstraint(Point2PointTypes.SYMMETRICPATH);
        if (symmetricPathConstraint != null) {
            symmetricPath = Boolean.valueOf(symmetricPathConstraint.getValue());
        }

        // Get source stpId.
        StringAttrConstraint sourceStp = constraints.getStringAttrConstraint(Point2PointTypes.SOURCESTP);
        if (sourceStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null"));
        }
        String srcStpId = sourceStp.getValue();

        // Get destination stpId.
        StringAttrConstraint destStp = constraints.getStringAttrConstraint(Point2PointTypes.DESTSTP);
        if (destStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null"));
        }
        String dstStpId = destStp.getValue();

        // TODO: Need to handle underspecified STPid.

        // Get the topology model used for routing.
        NsiTopology nsiTopology = pceData.getTopology();

        log.debug("******* localId " + nsiTopology.getLocalNsaId());
        for (String networkId : nsiTopology.getNetworkIds()) {
            log.debug("******* networkId " + networkId);
        }

        // Look up the STP within our model matching the request.
        StpType srcStp = nsiTopology.getStp(srcStpId);
        StpType dstStp = nsiTopology.getStp(dstStpId);

        // TODO: If we decide to allow blind routing to a network then remove
        // these tests for a null STP.
        if (srcStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), srcStpId));
        }
        else if (dstStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), dstStpId));
        }

        // Verify the specified STP are of the correct type for the request.
        if (directionality == DirectionalityType.UNIDIRECTIONAL) {
             if (srcStp.getType() != StpDirectionalityType.INBOUND &&
                     srcStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), srcStpId));
            }

            if (dstStp.getType() != StpDirectionalityType.INBOUND &&
                     dstStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), dstStpId));
            }
        }
        else {
            if (srcStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), srcStpId));
            }

            if (dstStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), dstStpId));
            }
        }

        // These will be applied to individual path segment results.
        Constraints segmentConstraints = new Constraints(pceData.getConstraints());
        segmentConstraints.removeStringAttrConstraint(Point2PointTypes.SOURCESTP);
        segmentConstraints.removeStringAttrConstraint(Point2PointTypes.DESTSTP);

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

        // We build a graph consisting of source STP, destination STP, and all
        // service domains as verticies.  The SDP are added as edges between
        // service domains.  A special edge is added between the edge STP and
        // the first service domain to allow for path finding between the source
        // and destination STP.  We will need to remove any SDP that are
        // associated with the source or destination STP.
        Graph<DijkstraVertex, DijkstraEdge> graph = new SparseMultigraph<>();
        Transformer<DijkstraEdge,Number> trans = new DijkstraTrasnsformer(pceData);

        // Add the source and destination STP as verticies.
        DijkstraVertex srcVertex = new DijkstraVertex(srcStp.getId(), srcStp);
        graph.addVertex(srcVertex);
        verticies.put(srcVertex.getId(), srcVertex);

        DijkstraVertex dstVertex = new DijkstraVertex(dstStp.getId(), dstStp);
        graph.addVertex(dstVertex);
        verticies.put(dstVertex.getId(), dstVertex);

        // Add ServiceDomains as verticies.
        for (ServiceDomainType serviceDomain : nsiTopology.getServiceDomains()) {
            DijkstraVertex vertex = new DijkstraVertex(serviceDomain.getId(), serviceDomain);
            graph.addVertex(vertex);
            verticies.put(vertex.getId(), vertex);
        }

        // Connect the source and destination STP verticies to their associated
        // service domains.
        DijkstraEdge sourceEdge = new DijkstraEdge(srcStp.getId(), srcStp, sourceServiceDomain);
        graph.addEdge(sourceEdge, srcVertex, verticies.get(sourceServiceDomain.getId()));
        DijkstraEdge destinationEdge = new DijkstraEdge(dstStp.getId(), dstStp, destinationServiceDomain);
        graph.addEdge(destinationEdge, dstVertex, verticies.get(destinationServiceDomain.getId()));

        // Add bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                Optional<ResourceRefType> aServiceDomainRef = Optional.fromNullable(sdp.getDemarcationA().getServiceDomain());
                Optional<ResourceRefType> zServiceDomainRef = Optional.fromNullable(sdp.getDemarcationZ().getServiceDomain());
                Optional<ResourceRefType> srcSdp = Optional.fromNullable(srcStp.getSdp());
                Optional<ResourceRefType> dstSdp = Optional.fromNullable(dstStp.getSdp());

                if (!aServiceDomainRef.isPresent()) {
                    log.error("Missing service domain for demarcationA sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationA().getStp().getId());
                }
                else if (!zServiceDomainRef.isPresent()) {
                    log.error("Missing service domain for demarcationZ sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationZ().getStp().getId());
                }
                else if (srcSdp.isPresent() && srcSdp.get().getId().equalsIgnoreCase(sdp.getId())) {
                    log.debug("Omitting SDP sdpId=" + sdp.getId() + ", due to source stpId=" + srcStp.getId());
                }
                else if (dstSdp.isPresent() && dstSdp.get().getId().equalsIgnoreCase(sdp.getId())) {
                    log.debug("Omitting SDP sdpId=" + sdp.getId() + ", due to destination stpId=" + dstStp.getId());
                }
                else {
                    DijkstraEdge sdpEdge = new DijkstraEdge(sdp.getId(), sdp);
                    graph.addEdge(sdpEdge, verticies.get(aServiceDomainRef.get().getId()), verticies.get(zServiceDomainRef.get().getId()));
                }
            }
        }

        DijkstraShortestPath<DijkstraVertex, DijkstraEdge> alg = new DijkstraShortestPath<>(graph);

        List<DijkstraEdge> path;
        try {

            path = alg.getPath(srcVertex, dstVertex);
        } catch (IllegalArgumentException ex) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, ex.getMessage());
            log.error(error, ex);
            throw new IllegalArgumentException(error);
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");

        // Check to see if there is a valid path.
        if (path.isEmpty()) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "No path found using provided criteria");
            throw new RuntimeException(error);
        }

        List<StpPair> segments = pullIndividualSegmentsOut(path, nsiTopology);
        for (int i = 0; i < segments.size(); i++) {
            StpPair pair = segments.get(i);

            log.debug("Pair: " + pair.getA().getId() + " -- " + pair.getZ().getId());

            Constraints cons = new Constraints(segmentConstraints);
            PathSegment pathSegment = new PathSegment(pair);
            pathSegment.setConstraints(cons);
            pceData.getPath().getPathSegments().add(i, pathSegment);
        }

        return pceData;
    }

    protected List<StpPair> pullIndividualSegmentsOut(List<DijkstraEdge> path, NsiTopology nsiTopology) {
        List<StpPair> segments = new ArrayList<>();

        Optional<StpType> start = Optional.absent();
        for (DijkstraEdge edge: path) {
            log.debug("--- Edge: " + edge.getId());

            if (edge.getType() == DijkstraEdgeType.STP_SERVICEDOMAIN) {
                if (!start.isPresent()) {
                    // Source STP in the path.
                    start = Optional.of(edge.getStp());
                }
                else {
                    // Destimnation STP in the path.
                    segments.add(new StpPair(start.get(), edge.getStp()));
                }
            }
            else if (edge.getType() == DijkstraEdgeType.SDP) {
                // SDP along the path.
                StpType stpA = nsiTopology.getStp(edge.getSdp().getDemarcationA().getStp().getId());
                StpType stpZ = nsiTopology.getStp(edge.getSdp().getDemarcationZ().getStp().getId());

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
