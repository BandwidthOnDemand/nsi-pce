package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SortedSparseMultigraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.path.services.Service;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
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

        String serviceType = PfUtils.getServiceTypeOrFail(constraints);
        List<Service> serviceByType = Service.getServiceByType(serviceType);
        if (!serviceByType.contains(Service.P2PS)) {
            throw Exceptions.unsupportedParameter(PCEConstraints.NAMESPACE, PCEConstraints.SERVICETYPE, serviceType);
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
            log.error("DijkstraPCE: Failed to parse source STP: " + sourceStp, ex);
            throw Exceptions.stpResolutionError(sourceStp);
        }

        // Parse the destination STP to make sure it is valid.
        SimpleStp dstStpId;
        try {
            dstStpId = new SimpleStp(destStp);
        }
        catch (IllegalArgumentException ex) {
            log.error("DijkstraPCE: Failed to parse destination STP: " + sourceStp, ex);
            throw Exceptions.stpResolutionError(destStp);
        }

        // Get the topology model used for routing.
        NsiTopology nsiTopology = pceData.getTopology();

        log.debug("******* localId " + nsiTopology.getLocalNsaId());
        for (String networkId : nsiTopology.getNetworkIds()) {
            log.debug("******* networkId " + networkId);
        }

        List<GraphEdge> path = getPath(srcStpId, dstStpId, directionality, pceData, nsiTopology);

        // Check to see if there is a valid path.
        if (path == null || path.isEmpty()) {
            throw Exceptions.noPathFound("No path found using provided criteria");
        }

        List<StpPair> segments = pullIndividualSegmentsOut(path, nsiTopology);
        for (int i = 0; i < segments.size(); i++) {
            StpPair pair = segments.get(i);

            log.debug("Pair: " + pair.getA().getId() + " -- " + pair.getZ().getId());

            // Copy the applicable attribute constraints into this path.
            AttrConstraints newConstraints = new AttrConstraints(pceData.getConstraints());
            newConstraints.removeObjectAttrConstraint(Point2PointTypes.P2PS);

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
            newConstraints.add(p2psObject);

            PathSegment pathSegment = new PathSegment(pair);
            pathSegment.setConstraints(newConstraints);
            pceData.getPath().getPathSegments().add(i, pathSegment);
        }

        return pceData;
    }

    private List<GraphEdge> getPath(SimpleStp srcStpId, SimpleStp dstStpId, DirectionalityType directionality, PCEData pceData, NsiTopology nsiTopology) throws WebApplicationException {
        // We build a graph consisting of source STP, destination STP, and all
        // service domains as verticies.  The SDP are added as edges between
        // service domains.  A special edge is added between the edge STP and
        // the first service domain to allow for path finding between the source
        // and destination STP.  We will need to remove any SDP that are
        // associated with the source or destination STP.
        Map<String, GraphVertex> verticies = new HashMap<>();
        Graph<GraphVertex, GraphEdge> graph = new SortedSparseMultigraph<>();
        Transformer<GraphEdge, Number> trans = new EdgeTrasnsformer(pceData);

        // There are four possible cases for our request STP endpoints:
        //     1. A fully qualified STP that exists in topology.
        //     2. A fully qualified STP that does not exist in topology.
        //     3. An underspecified STP that contains members in topology.
        //     4. An underspecified STP that contains members that do not exist
        //        in topology.
        //
        // Endpoints that do not existing in topology cause issues as they are
        // not connected to ServiceDomains, and therefore, we do not know
        // which ServiceDomain in the local network should be used as a start
        // for path finding.
        //
        // TODO: Add path finding support for STP not in topology.

        // Get the set of possible source and destination ports.
        StpTypeBundle srcBundle = new StpTypeBundle(nsiTopology, srcStpId, directionality);
        if (srcBundle.isEmpty()) {
            log.error("getPath: source STP does not exist in topology: " + srcStpId.toString());
            throw Exceptions.stpResolutionError(srcStpId.toString());
        }

        StpTypeBundle dstBundle = new StpTypeBundle(nsiTopology, dstStpId, directionality);
        if (dstBundle.isEmpty()) {
            log.error("getPath: destination STP does not exist in topology: " + dstStpId.toString());
            throw Exceptions.stpResolutionError(dstStpId.toString());
        }

        // Build the graph.
        addStpVerticies(graph, verticies, srcBundle);
        addStpVerticies(graph, verticies, dstBundle);
        addServiceDomainVerticies(graph, verticies, nsiTopology.getServiceDomains());
        linkStpAndServiceDomainVerticies(nsiTopology, graph, verticies, srcBundle, nsiTopology.getServiceDomains());
        linkStpAndServiceDomainVerticies(nsiTopology, graph, verticies, dstBundle, nsiTopology.getServiceDomains());
        Set<String> exclusionSdp = getExclusionSdp(nsiTopology, srcBundle);
        exclusionSdp.addAll(getExclusionSdp(nsiTopology, dstBundle));
        addSdpEdges(nsiTopology, graph, verticies, exclusionSdp);
        DijkstraShortestPath<GraphVertex, GraphEdge> alg = new DijkstraShortestPath<>(graph, trans, true);

        // We now try to find a viable path based on the graph we just built.
        // If there is only a single source and destination STP then this is
        // simple, however, if we have multiple then we need to iterate through
        // the combinations.  Randomize to give us a good chance of success when
        // singalling to the uPA.
        List<GraphEdge> path = new LinkedList<>();
        StpChooser pairs = new StpChooser(srcBundle, dstBundle);
        while (pairs.hasNext()) {
            try {
                StpPair pair = pairs.removeRandom();
                log.debug("getPath: stpA=" + pair.getA().getId() + ", stpZ=" + pair.getZ().getId());
                path = alg.getPath(verticies.get(pair.getA().getId()), verticies.get(pair.getZ().getId()));
            } catch (IllegalArgumentException ex) {
                log.error("No path found", ex);
                throw Exceptions.noPathFound(ex.getMessage());
            }

            if (!path.isEmpty()) {
                break;
            }
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");

        return path;
    }

    private ServiceDomainType getServiceDomainOrFail(NsiTopology topology, StpType stp) {
        Optional<ResourceRefType> serviceDomain = Optional.fromNullable(stp.getServiceDomain());
        if (serviceDomain.isPresent()) {
            Optional<ServiceDomainType> sd = Optional.fromNullable(topology.getServiceDomain(stp.getServiceDomain().getId()));
            if (sd.isPresent()) {
                return sd.get();
            }
        }
        throw Exceptions.noPathFound("Missing ServiceDomain for source sdpId=" + stp.getId());
    }

    private void addStpVerticies(Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, StpTypeBundle bundle) {
        for (StpType stp : bundle.values()) {
            StpVertex srcVertex = new StpVertex(stp.getId(), stp);
            graph.addVertex(srcVertex);
            verticies.put(srcVertex.getId(), srcVertex);
        }
    }

    private void addServiceDomainVerticies(Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, Collection<ServiceDomainType> serviceDomains) {
        // Add ServiceDomains as verticies.
        for (ServiceDomainType serviceDomain : serviceDomains) {
            ServiceDomainVertex vertex = new ServiceDomainVertex(serviceDomain.getId(), serviceDomain);
            graph.addVertex(vertex);
            verticies.put(vertex.getId(), vertex);
        }
    }

    private void linkStpAndServiceDomainVerticies(NsiTopology topology, Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, StpTypeBundle bundle, Collection<ServiceDomainType> serviceDomains) {
        for (StpType stp : bundle.values()) {
            ServiceDomainType serviceDomain = getServiceDomainOrFail(topology, stp);
            StpEdge edge = new StpEdge(stp.getId(), stp, serviceDomain);
            GraphVertex vertex = verticies.get(stp.getId());
            graph.addEdge(edge, vertex, verticies.get(serviceDomain.getId()));
        }
    }

    private Set<String> getExclusionSdp(NsiTopology topology, StpTypeBundle stpBundle) {
        Set<String> exclusionSdp = new HashSet<>();
        Optional<Map<String, StpType>> bundle = Optional.fromNullable(topology.getStpBundle(stpBundle.getSimpleStp().getId()));
        if (bundle.isPresent()) {
            for (StpType anStp : bundle.get().values()) {
                Optional<ResourceRefType> sdpRef = Optional.fromNullable(anStp.getSdp());
                if (sdpRef.isPresent()) {
                    exclusionSdp.add(sdpRef.get().getId());
                }
            }
        }

        return exclusionSdp;
    }

    private void addSdpEdges(NsiTopology topology, Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, Set<String> exclusionSdp) {
        // We only do bidirectional path finding at this point so add the
        // bidirectional SDP as edges.
        for (SdpType sdp : topology.getSdps()) {
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
                    log.debug("Omitting SDP sdpId=" + sdp.getId());
                }
                else {
                    SdpEdge sdpEdge = new SdpEdge(sdp.getId(), sdp);
                    graph.addEdge(sdpEdge, verticies.get(aServiceDomainRef.get().getId()), verticies.get(zServiceDomainRef.get().getId()));
                }
            }
        }
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
