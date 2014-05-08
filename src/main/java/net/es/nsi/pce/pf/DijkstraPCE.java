package net.es.nsi.pce.pf;

import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
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
import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;

/**
 * Main path computation class using Dijkstra's shortest path on an NSI
 * topology model.
 *
 * @author hacksaw
 */
public class DijkstraPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());

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
        StringAttrConstraint directionalityConstraint = constraints.getStringAttrConstraint(Point2Point.DIRECTIONALITY);
        if (directionalityConstraint != null) {
            directionality = DirectionalityType.valueOf(directionalityConstraint.getValue());
        }

        // Determine path symmetry.
        boolean symmetricPath = true;
        BooleanAttrConstraint symmetricPathConstraint = constraints.getBooleanAttrConstraint(Point2Point.SYMMETRICPATH);
        if (symmetricPathConstraint != null) {
            symmetricPath = Boolean.valueOf(symmetricPathConstraint.getValue());
        }

        // Get source stpId.
        StringAttrConstraint sourceStp = constraints.getStringAttrConstraint(Point2Point.SOURCESTP);
        if (sourceStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2Point.NAMESPACE, Point2Point.SOURCESTP));
        }
        String srcStpId = sourceStp.getValue();

        // Get destination stpId.
        StringAttrConstraint destStp = constraints.getStringAttrConstraint(Point2Point.DESTSTP);
        if (destStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2Point.NAMESPACE, Point2Point.DESTSTP));
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
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.SOURCESTP, srcStpId));
        }
        else if (dstStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.DESTSTP, dstStpId));
        }

        // Verify the specified STP are of the correct type for the request.
        if (directionality == DirectionalityType.UNIDIRECTIONAL) {
             if (srcStp.getType() != StpDirectionalityType.INBOUND &&
                     srcStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2Point.SOURCESTP, srcStpId));
            }

            if (dstStp.getType() != StpDirectionalityType.INBOUND &&
                     dstStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2Point.DESTSTP, dstStpId));
            }
        }
        else {
            if (srcStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2Point.SOURCESTP, srcStpId));
            }

            if (dstStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2Point.DESTSTP, dstStpId));
            }
        }

        // These will be applied to individual path segment results.
        Constraints segmentConstraints = new Constraints(pceData.getConstraints());
        segmentConstraints.removeStringAttrConstraint(Point2Point.SOURCESTP);
        segmentConstraints.removeStringAttrConstraint(Point2Point.DESTSTP);

        // We can save time by handling the special case of A and Z STP in same
        // network.
        String srcNetwork = srcStp.getNetworkId();
        String dstNetwork = dstStp.getNetworkId();

        if (srcNetwork.equals(dstNetwork)) {
            StpPair pathPair = new StpPair(srcStp, dstStp);
            PathSegment pathSegment = new PathSegment(pathPair);
            Constraints cons = new Constraints(segmentConstraints);
            pathSegment.setConstraints(cons);
            pceData.getPath().getPathSegments().add(pathSegment);
            return pceData;
        }

        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<ServiceDomainType, SdpType> graph = new SparseMultigraph<>();

        // Add ServiceDomains as verticies.
        for (ServiceDomainType serviceDomain : nsiTopology.getServiceDomains()) {
            //log.debug("Adding Vertex: " + serviceDomain.getId());
            graph.addVertex(serviceDomain);
        }

        // Add bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                if (sdp.getDemarcationA().getServiceDomain() == null) {
                    log.error("Missing service domain for demarcationA sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationA().getStp().getId());
                }
                else if (sdp.getDemarcationZ().getServiceDomain() == null) {
                    log.error("Missing service domain for demarcationZ sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationZ().getStp().getId());
                }
                else {
                    ServiceDomainType aServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
                    ServiceDomainType bServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());
                    graph.addEdge(sdp, aServiceDomain, bServiceDomain);
                }
            }
        }

        DijkstraShortestPath<ServiceDomainType, SdpType> alg = new DijkstraShortestPath<>(graph);

        List<SdpType> path;
        try {
            ServiceDomainType sourceServiceDomain = nsiTopology.getServiceDomain(srcStp.getServiceDomain().getId());
            ServiceDomainType destinationServiceDomain = nsiTopology.getServiceDomain(dstStp.getServiceDomain().getId());
            path = alg.getPath(sourceServiceDomain, destinationServiceDomain);
        } catch (IllegalArgumentException ex) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "DijkstraPCE", ex.getMessage());
            log.error(error, ex);
            throw new IllegalArgumentException(error);
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");

        // Check to see if there is a valid path.
        if (path.isEmpty()) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "DijkstraPCE", "No path found using provided criteria");
            throw new RuntimeException(error);
        }

        List<StpPair> segments = pullIndividualSegmentsOut(srcStp, dstStp, path, nsiTopology);
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

    protected List<StpPair> pullIndividualSegmentsOut(StpType srcStp, StpType dstStp, List<SdpType> path, NsiTopology nsiTopology) {
        List<StpPair> segments = new ArrayList<>();

        StpType start = srcStp;
        for (SdpType edge: path) {
            log.debug("--- Edge: " + edge.getId());
            StpType stpA = nsiTopology.getStp(edge.getDemarcationA().getStp().getId());
            StpType stpZ = nsiTopology.getStp(edge.getDemarcationZ().getStp().getId());

            StpPair pathPair;
            if (start.getNetworkId().equalsIgnoreCase(stpA.getNetworkId())) {
                pathPair = new StpPair(start, stpA);
                start = stpZ;
            }
            else {
                pathPair = new StpPair(start, stpZ);
                start = stpA;
            }
            segments.add(pathPair);
        }

        segments.add(new StpPair(start, dstStp));

        return segments;
    }
}
