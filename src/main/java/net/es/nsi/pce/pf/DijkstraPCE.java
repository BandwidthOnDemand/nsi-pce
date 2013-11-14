package net.es.nsi.pce.pf;

import java.util.List;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;

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
            throw new IllegalArgumentException("No path endpoints found in request.");
        }

        // Verify both networks in request are known in our topology.
        NsiTopology nsiTopology = pceData.getTopology();
        NetworkType srcNetwork = nsiTopology.getNetworkById(pe.getSrcNetwork());
        NetworkType dstNetwork = nsiTopology.getNetworkById(pe.getDstNetwork());

        if (srcNetwork == null) {
            throw new IllegalArgumentException("Unknown source network " + pe.getSrcNetwork());
        }
        else if (dstNetwork == null) {
            throw new IllegalArgumentException("Unknown destination network " + pe.getDstNetwork());
        }
        
        // TODO: Need to make this a generic label switching path finder!
        
        // Build the STP identifiers using local Id and vlan Ids.
        String srcStpId = nsiTopology.newStpId(pe.getSrcNetwork(), pe.getSrcLocal(), pe.getSrcLabel());
        String dstStpId = nsiTopology.newStpId(pe.getDstNetwork(), pe.getDstLocal(), pe.getDstLabel());
        
        // Look up the STP within our model matching the request.
        StpType srcStp = nsiTopology.getStp(srcStpId);
        StpType dstStp = nsiTopology.getStp(dstStpId);

        // TODO: If we decide to allow blind routing to a network then remove
        // these tests for a null STP.
        if (srcStp == null) {
            throw new IllegalArgumentException("Unknown source STP " + srcStpId);
        }
        else if (dstStp == null) {
            throw new IllegalArgumentException("Unknown destination STP " + dstStpId);
        }

        // We currently do not support label swapping so make sure the source
        // and destination labels match.  This restriction can be removed later
        // when the TransferService is introdcued.
        if (!nsiTopology.labelEquals(srcStp.getLabel(), dstStp.getLabel())) {
            IllegalArgumentException ex = new IllegalArgumentException("Source and destination STP label mismatch");
            log.error("Path computation failed due to label mismatch", ex);
            throw ex;                 
        }
        
        // We can save time by handling the special case of A and Z STP in same
        // network.
        if (srcNetwork.equals(dstNetwork)) {
            StpPair pathPair = new StpPair();
            pathPair.setA(srcStp);
            pathPair.setZ(dstStp);
            pceData.getPath().getStpPairs().add(pathPair);
            return pceData;
        }

        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<NetworkType, SdpType> graph = new SparseMultigraph<>();
        
        // Add Networks as verticies.
        for (NetworkType network : nsiTopology.getNetworks()) {
            log.debug("Adding Vertex: " + network.getId());
            graph.addVertex(network);
        }        

        // Add bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                // Get the component STP of this edge.
                StpType stpA = nsiTopology.getStp(sdp.getStpA().getId());
                StpType stpZ = nsiTopology.getStp(sdp.getStpZ().getId());
                
                // Until the TransferService is supported we must filter edges
                // to match the label of source and destination STP.
                if (nsiTopology.labelEquals(srcStp.getLabel(), stpA.getLabel())) {
                    graph.addEdge(sdp, nsiTopology.getNetworkById(stpA.getNetworkId()), nsiTopology.getNetworkById(stpZ.getNetworkId()));
                }                         
            }
        }
 
        // Verify that the source and destination STP are still in our topology.
        if (!graph.containsVertex(nsiTopology.getNetworkById(srcStp.getNetworkId()))) {
            throw new IllegalArgumentException("Source STP is not in computed topology: " + srcStp);
        } else if (!graph.containsVertex(nsiTopology.getNetworkById(dstStp.getNetworkId()))) {
            throw new IllegalArgumentException("Destination STP is not in computed topology: " + dstStp);
        }

        DijkstraShortestPath<NetworkType, SdpType> alg = new DijkstraShortestPath<>(graph);
        
        List<SdpType> path;
        try {
            path = alg.getPath(nsiTopology.getNetworkById(srcStp.getNetworkId()), nsiTopology.getNetworkById(dstStp.getNetworkId()));
        } catch (Exception ex) {
            log.error("Path computation failed", ex);
            throw ex;
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");
        
        // Check to see if there is a valid path.
        if (path.isEmpty()) {
            throw new Exception("No path found using provided criteria");
        }
        
        // Now we pull the individual edge segments out of the result and
        // determine the component STPs.
        int i = 0;
        StpPair pathPair = new StpPair();
        pathPair.setA(srcStp);
        for (SdpType edge: path) {
            log.debug("--- Edge: " + edge.getId());
            StpPair nextPathPair = new StpPair();
            StpType stpA = nsiTopology.getStp(edge.getStpA().getId());
            StpType stpZ = nsiTopology.getStp(edge.getStpZ().getId());
                    
            if (pathPair.getA().getNetworkId().equalsIgnoreCase(stpA.getNetworkId())) {
                pathPair.setZ(stpA);
                nextPathPair.setA(stpZ);
            }
            else {
                pathPair.setZ(stpZ);
                nextPathPair.setA(stpA);
            }

            pceData.getPath().getStpPairs().add(i, pathPair);
            pathPair = nextPathPair;
            i++;
        }
        pathPair.setZ(dstStp);
        pceData.getPath().getStpPairs().add(i, pathPair);

        for (StpPair pair : pceData.getPath().getStpPairs()) {
            log.debug("Pair: " + pair.getA().getId() + " -- " + pair.getZ().getId());
        }
        return pceData;
    }

}
