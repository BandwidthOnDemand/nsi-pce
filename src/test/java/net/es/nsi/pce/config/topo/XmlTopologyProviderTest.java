/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import java.util.List;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Sdp;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.test.TestConfig;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 *
 * @author hacksaw
 */
public class XmlTopologyProviderTest {
    @Test
    public void loadTopology() throws Exception {
       
        TopologyProvider provider = TestConfig.getInstance().getTopologyProvider();
        
        try {
            provider.loadTopology();
        }
        catch (Exception ex) {
            System.err.println("loadTopology() Failed: " + ex.getMessage());
            fail();
        }
        
        System.out.println("Loaded network topologies:");
        for (Network network : provider.getNetworks()) {
            System.out.println("---- " + network.getNetworkId());
            try {
                for (Stp stp : network.getStps()) {
                    System.out.print("      " + stp.getId());
                    if (stp.getRemoteStp() != null) {
                        System.out.print(" --> " + stp.getRemoteStp().getId());
                    }
                    System.out.println();
                }
            } catch (Exception ex) {
                System.err.println("Dump of topology failed: " + ex.getMessage());
                fail();
            }
        }
    }
    
    @Test
    public void testPath() {
        TopologyProvider provider = TestConfig.getInstance().getTopologyProvider();
        
        try {
            provider.loadTopology();
        }
        catch (Exception ex) {
            System.err.println("loadTopology() Failed: ");
            ex.printStackTrace();
            fail();
        }
        
        System.out.println("Building graph...");
        
        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<Network, Sdp> graph = new SparseMultigraph<Network, Sdp>();
        
        // Add Networks as verticies first.
        for (Network network : provider.getNetworks()) {
            System.out.println("Adding Vertex: " + network.getNetworkId());
            graph.addVertex(network);
        }
        
        // Add SDP as edges.
        for (Sdp sdp : provider.getTopology().getSdps()) {
            System.out.println("Adding Edge: " + sdp.getId());
            graph.addEdge(sdp, sdp.getA().getNetwork(), sdp.getZ().getNetwork());
        }
        
        Network a = provider.getTopology().getNetworkById("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
        Network z = provider.getTopology().getNetworkById("urn:ogf:network:kddilabs.jp:2013:topology");
        
        @SuppressWarnings("unchecked")
        DijkstraShortestPath<Network,Sdp> alg = new DijkstraShortestPath(graph);
        List<Sdp> list = alg.getPath(a, z);

        System.out.println("The shortest unweighted path from " + a + " to " + z + " is:");
        for (Sdp sdp : list) {
            System.out.println("  " + sdp.getId());
        }
    }
}
