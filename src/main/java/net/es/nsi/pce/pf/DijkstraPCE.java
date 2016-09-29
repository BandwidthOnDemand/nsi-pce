package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SortedSparseMultigraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.topology.ResourceRefType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
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
    private final net.es.nsi.pce.jaxb.path.ObjectFactory factory = new net.es.nsi.pce.jaxb.path.ObjectFactory();
    
    /**
     * This path computation module supports pathfinding for the P2PS service
     * specification restricted to bidirectional symmetricPath services with
     * two stpId specified in the request.
     *
     * This module does not currently use the following parameters:
     *    - startTime
     *    - endTime
     *    - capacity
     *
     * @param pceData
     * @return
     */
    @Override
    public PCEData apply(PCEData pceData) {
        checkNotNull(pceData.getTopology(), "DijkstraPCE: No topology was provided");

        // Extract the request data we need.
        Request request = new Request(pceData);

        // Find a path satisfying the requested criteria.
        List<GraphEdge> path = route(request);

        // Check to see if there is a valid path.
        if (path.isEmpty()) {
            throw Exceptions.noPathFound("No path found using provided criteria");
        }

        // Consolidate the path segment results.
        List<GraphEdge> consolidatedPath = consolidatePath(request.getNsiTopology(), path);
        List<StpPair> individualSegments = getIndividualSegments(request.getNsiTopology(), consolidatedPath);

        // Build a path response for each segment in the the computed path.
        return build(request, individualSegments);
    }
    
    private List<GraphEdge> route(Request request) {
        // Perform pathfinding on each computed route.
        List<GraphEdge> path = new ArrayList<>();
        Optional<StpType> nextStp = Optional.absent();
        Iterator<Route> iterator = request.getRo().getRoutes().iterator();
        while (iterator.hasNext()) {
            // We will process this route now.
            Route route = iterator.next();

            // Manipulate the sourceBundle if we had a previous iteration has
            // restricted the STP's available on this bundle.
            StpTypeBundle sourceBundle = route.getBundleA().getPeerRestrictedBundle(request.getNsiTopology(), nextStp, request.getDirectionality());

            // Compute the shortest path.
            List<GraphEdge> edges = getShortestPath(sourceBundle, route.getBundleZ(), request.getPceData(), request.getNsiTopology());

            // Check to see if we found a valid path.
            if (edges.isEmpty()) {
                throw Exceptions.noPathFound("No path found using provided criteria sourceStp=" + route.getBundleA().getSimpleStp().getId() + ", and destStp=" + route.getBundleZ().getSimpleStp().getId());
            }

            // Extract the computed destination STP here so we can get the
            // STP on the end of the SDP for the start of our next iteration.
            if (iterator.hasNext()) {
                GraphEdge terminalEdge = edges.get(edges.size() - 1);
                if (terminalEdge instanceof StpEdge) {
                    StpEdge stpEdge = (StpEdge) terminalEdge;
                    nextStp = Optional.of(stpEdge.getStp());
                }
                else {
                    throw Exceptions.noPathFound("Issue with edge STP: " + route.getBundleZ().getSimpleStp().getStpId());
                }
            }

            // Add edges from this path computation to our result set.
            path.addAll(edges);
        }
        
        return path;
    }

    private List<GraphEdge> getShortestPath(StpTypeBundle srcBundle,
            StpTypeBundle dstBundle, PCEData pceData, NsiTopology nsiTopology)
            throws WebApplicationException {
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

        // Build the graph.
        GraphUtils.addStpVerticies(graph, verticies, srcBundle);
        GraphUtils.addStpVerticies(graph, verticies, dstBundle);
        GraphUtils.addServiceDomainVerticies(graph, verticies, nsiTopology.getServiceDomains());
        GraphUtils.linkStpAndServiceDomainVerticies(nsiTopology, graph, verticies, srcBundle, nsiTopology.getServiceDomains());
        GraphUtils.linkStpAndServiceDomainVerticies(nsiTopology, graph, verticies, dstBundle, nsiTopology.getServiceDomains());
        Set<String> exclusionSdp = nsiTopology.getExclusionSdp(srcBundle);
        exclusionSdp.addAll(nsiTopology.getExclusionSdp(dstBundle));
        GraphUtils.addSdpEdges(nsiTopology, graph, verticies, exclusionSdp);
        DijkstraShortestPath<GraphVertex, GraphEdge> alg = new DijkstraShortestPath<>(graph, trans, true);

        // We now try to find a viable path based on the graph we just built.
        // If there is only a single source and destination STP then this is
        // simple, however, if we have multiple then we need to iterate through
        // the combinations.  Randomize to give us a good chance of success when
        // singalling to the uPA.  We will try every possible pair to get the
        // shortest path.
        List<GraphEdge> shortestPath = new ArrayList<>();
        StpChooser pairs = new StpChooser(srcBundle, dstBundle);
        while (pairs.hasNext()) {
            List<GraphEdge> path;
            try {
                StpPair pair = pairs.removeRandom();
                log.debug("getPath: trying stpA=" + pair.getA().getId() + ", stpZ=" + pair.getZ().getId());
                path = alg.getPath(verticies.get(pair.getA().getId()), verticies.get(pair.getZ().getId()));
            } catch (IllegalArgumentException ex) {
                log.error("No path found", ex);
                throw Exceptions.noPathFound(ex.getMessage());
            }

            if (!path.isEmpty() && (shortestPath.isEmpty() || path.size() < shortestPath.size())) {
                shortestPath = path;
            }
        }

        log.debug("Path computation completed with " + shortestPath.size() + " SDP returned.");
        return shortestPath;
    }

    private List<GraphEdge> consolidatePath(NsiTopology topology, List<GraphEdge> path) {
        // We now consolidate adjacent STP at ERO breakpoints back into SDP.
        List<GraphEdge> consolidatedPath = new ArrayList<>();
        Iterator<GraphEdge> edge = path.iterator();
        GraphEdge lastEdge = null;
        while (edge.hasNext()) {
            GraphEdge currentEdge = edge.next();
            if (lastEdge != null) {
                if (currentEdge instanceof StpEdge && lastEdge instanceof StpEdge) {
                    StpEdge currentStpEdge = (StpEdge) currentEdge;
                    StpEdge lastStpEdge = (StpEdge) lastEdge;

                    Optional<ResourceRefType> currentSdpRef = Optional.fromNullable(currentStpEdge.getStp().getSdp());
                    Optional<ResourceRefType> lastSdpRef = Optional.fromNullable(lastStpEdge.getStp().getSdp());

                    if (currentSdpRef.isPresent() && lastSdpRef.isPresent() && currentSdpRef.get().getId().equalsIgnoreCase(lastSdpRef.get().getId())) {
                        Optional<SdpType> sdp = Optional.fromNullable(topology.getSdp(currentSdpRef.get().getId()));
                        if (!sdp.isPresent()) {
                            throw Exceptions.noPathFound("No SDP associated with STP: " + currentStpEdge.getStp().getId());
                        }

                        SdpEdge sdpEdge = new SdpEdge(sdp.get());
                        consolidatedPath.add(sdpEdge);
                        lastEdge = null;
                        continue;
                    }
                }

                consolidatedPath.add(lastEdge);
            }
            lastEdge = currentEdge;
        }

        if(lastEdge != null) {
            consolidatedPath.add(lastEdge);
        }

        return consolidatedPath;
    }

    protected List<StpPair> getIndividualSegments(NsiTopology topology, List<GraphEdge> path) {
        List<StpPair> segments = new ArrayList<>();

        Optional<StpType> start = Optional.absent();
        for (GraphEdge edge: path) {
            if (edge instanceof StpEdge) {
                StpEdge stpEdge = (StpEdge) edge;

                if (!start.isPresent()) {
                    // Source STP in the path.
                    start = Optional.of(stpEdge.getStp());
                }
                else {
                    // Destination STP in the path.
                    segments.add(new StpPair(start.get(), stpEdge.getStp()));
                }
            }
            else if (edge instanceof SdpEdge) {
                SdpEdge sdpEdge = (SdpEdge) edge;

                // SDP along the path.
                StpType stpA = topology.getStp(sdpEdge.getSdp().getDemarcationA().getStp().getId());
                StpType stpZ = topology.getStp(sdpEdge.getSdp().getDemarcationZ().getStp().getId());

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
    
    public PCEData build(Request request, List<StpPair> segments) {
        for (int i = 0; i < segments.size(); i++) {
            StpPair pair = segments.get(i);

            String stpIdA = pair.getA().getId();
            String stpIdZ = pair.getZ().getId();

            log.debug("Pair: " + stpIdA + " -- " + stpIdZ);

            // Map the A and Z ends of the original request back to their initial
            // form just in case they were underspecified.
            String baseA = SimpleStp.getId(stpIdA);
            if (request.getSrcStpId().getId().equalsIgnoreCase(baseA)) {
                stpIdA = request.getSourceStp();
            }
            else if (request.getDstStpId().getId().equalsIgnoreCase(baseA)) {
                stpIdA = request.getDestStp();
            }

            String baseZ = SimpleStp.getId(stpIdZ);
            if (request.getSrcStpId().getId().equalsIgnoreCase(baseZ)) {
                stpIdZ = request.getSourceStp();
            }
            else if (request.getDstStpId().getId().equalsIgnoreCase(baseZ)) {
                stpIdZ = request.getDestStp();
            }

            // Copy the applicable attribute constraints into this path.
            AttrConstraints newConstraints = new AttrConstraints(request.getPceData().getConstraints());
            newConstraints.removeObjectAttrConstraint(Point2PointTypes.P2PS);

            // Create a new P2PS constraint for this segment.
            P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
            p2ps.setCapacity(request.getP2p().getCapacity());
            p2ps.setDirectionality(request.getDirectionality());
            p2ps.setEro(request.getRo().getInternalERO(SimpleStp.parseNetworkId(stpIdA)));
            p2ps.setSymmetricPath(request.isSymmetricPath());
            p2ps.setSourceSTP(stpIdA);
            p2ps.setDestSTP(stpIdZ);
            ObjectAttrConstraint p2psObject = new ObjectAttrConstraint();
            p2psObject.setAttrName(Point2PointTypes.P2PS);
            p2psObject.setValue(p2ps);
            newConstraints.add(p2psObject);

            PathSegment pathSegment = new PathSegment.Builder()
                    .withA(stpIdA)
                    .withZ(stpIdZ)
                    .withNetworkId(pair.getA().getNetworkId())
                    .withConstraints(newConstraints)
                    .build();
            request.getPceData().getPath().getPathSegments().add(i, pathSegment);
        }

        return request.getPceData();
    }
}
