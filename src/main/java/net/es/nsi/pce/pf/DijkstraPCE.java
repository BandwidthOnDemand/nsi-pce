package net.es.nsi.pce.pf;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;

import java.util.List;
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Sdp;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.services.Point2Point;
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

    @Override
    public PCEData apply(PCEData pceData) throws Exception {
        // Get path endpoints from constraints.
        TopoPathEndpoints pe = null;
        for (Constraint c : pceData.getConstraints()) {
            if (c instanceof TopoPathEndpoints) {
                pe = (TopoPathEndpoints) c;
            }
        }

        // Malformed request.
        if (pe == null) {
            throw new IllegalArgumentException("No path endpoints found.");
        }
                
        Topology topo = pceData.getTopology();

        Network srcNet = topo.getNetwork(pe.getSrcNetwork());
        Network dstNet = topo.getNetwork(pe.getDstNetwork());

        if (srcNet == null) {
            throw new IllegalArgumentException("Unknown src network " + pe.getSrcNetwork());
        }
        else if (dstNet == null) {
            throw new IllegalArgumentException("Unknown dst network " + pe.getDstNetwork());
        }
        
        // Build the Ethernet STP identifiers using local Id and vlan Ids.
        String srcStpId = pe.getSrcLocal();
        String dstStpId = pe.getDstLocal();
        Integer srcVlan = Point2Point.getVlanLabel(pe.getSrcLabels());
        Integer dstVlan = Point2Point.getVlanLabel(pe.getDstLabels());

        // Build the internal format for STP id.
        if (srcStpId != null && srcVlan != null) {
            srcStpId = srcStpId + ":vlan=" + srcVlan.toString();
        }
        
        if (dstStpId != null && dstVlan != null) {
            dstStpId = dstStpId + ":vlan=" + dstVlan.toString();
        }
        
        // Look up the STP within our model matching the request.
        Stp srcStp = srcNet.getStp(srcStpId);
        Stp dstStp = dstNet.getStp(dstStpId);

        // TODO: If we decide to allow blind routing to a network then remove
        // these tests for a null STP.
        if (srcStp == null) {
            throw new IllegalArgumentException("Unknown source STP " + pe.getSrcLocal() + ", vlan=" + srcVlan);
        }
        else if (dstStp == null) {
            throw new IllegalArgumentException("Unknown destination STP " + pe.getDstLocal() + ", vlan=" + dstVlan);
        }
        
        log.debug("source STP:" + srcStp.getId());
        log.debug("destination STP:" + dstStp.getId());
        
        // We can save time by handling the special case of A and Z STP in same
        // network.
        if (srcNet.equals(dstNet)) {
            // At the moment the VLANs must match (we should check if
            // interchange is possible in the domain).
            if ((srcVlan == null && dstVlan != null) ||
                    (dstVlan == null && srcVlan != null)) {
                // One VLAN specified is bad for now.
                IllegalArgumentException ex = new IllegalArgumentException("Path computation failed: source and destination VLAN mismatch");
                log.error("Path computation failed", ex);
                throw ex;
            }
            else if ((srcVlan == null && dstVlan == null) ||
                    srcVlan.compareTo(dstVlan) == 0) {
                // VLANs are matching which is good.
                StpPair pathPair = new StpPair();
                pathPair.setA(srcStp);
                pathPair.setZ(dstStp);
                pceData.getPath().getStpPairs().add(pathPair);
                return pceData;
            }
            else {
                // Catch all bad for not matching VLANs.
                IllegalArgumentException ex = new IllegalArgumentException("Path computation failed: source and destination VLAN mismatch");
                log.error("Path computation failed", ex);
                throw ex;  
            }
        }

        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<Network, Sdp> graph = new SparseMultigraph<Network, Sdp>();
        
        // Add Networks as verticies.
        for (Network network : topo.getNetworks()) {
            log.debug("Adding Vertex: " + network.getNetworkId());
            graph.addVertex(network);
        }        

        // Add bidirectional SDP as edges.
        for (Sdp sdp : topo.getSdps()) {
            if (sdp.getDirectionality() == Directionality.bidirectional) {
                // If we have a VLAN restriction then filter SDP based on what is usable.
                if (srcVlan != null) {
                    if (sdp.getA().getVlanId() == srcVlan.intValue()) {
                        log.debug("Adding vlan filtered bidirectional edge to graph: " + sdp.getId());
                        graph.addEdge(sdp, sdp.getA().getNetwork(), sdp.getZ().getNetwork());                        
                    }
                }
                else {
                    log.debug("Adding bidirectional edge: " + sdp.getId());
                    graph.addEdge(sdp, sdp.getA().getNetwork(), sdp.getZ().getNetwork());
                }
            }
        }
 
        // Verify that the source and destination STP are still in our topology.
        if (!graph.containsVertex(srcStp.getNetwork())) {
            throw new IllegalArgumentException("Source STP is not contained in topology: " + srcStp);
        } else if (!graph.containsVertex(dstStp.getNetwork())) {
            throw new IllegalArgumentException("Destination STP is not contained in topology: " + dstStp);
        }

        @SuppressWarnings("unchecked")
        DijkstraShortestPath<Network,Sdp> alg = new DijkstraShortestPath(graph);
        
        List<Sdp> path;
        try {
            path = alg.getPath(srcStp.getNetwork(), dstStp.getNetwork());
        } catch (Exception ex) {
            log.error("Path computation failed", ex);
            throw ex;
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");
        
        // Check to see if there is a valid path.
        if (path.isEmpty()) {
            throw new Exception("No path found.");
        }
        
        int i = 0;
        StpPair pathPair = new StpPair();
        pathPair.setA(srcStp);
        for (Sdp edge: path) {
            log.debug("--- Edge: " + edge.getId());
            StpPair nextPathPair = new StpPair();
            
            if (pathPair.getA().getNetworkId().equalsIgnoreCase(edge.getA().getNetworkId())) {
                pathPair.setZ(edge.getA());
                nextPathPair.setA(edge.getZ());
            }
            else {
                pathPair.setZ(edge.getZ());
                nextPathPair.setA(edge.getA());
            }

            pceData.getPath().getStpPairs().add(i, pathPair);
            pathPair = nextPathPair;
            i++;
        }
        pathPair.setZ(dstStp);
        pceData.getPath().getStpPairs().add(i, pathPair);

        for (StpPair pair : pceData.getPath().getStpPairs()) {
            log.debug("Pair: " + pair.getA() + " -- " + pair.getZ());
        }
        return pceData;
    }

}
