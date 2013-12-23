/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import java.util.List;

import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.NetworkType;

import net.es.nsi.pce.topology.provider.TopologyProvider;
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
        Graph<NetworkType, SdpType> graph = new SparseMultigraph<>();
        
        // Add Networks as verticies first.
        for (NetworkType network : provider.getNetworks()) {
            System.out.println("Adding Vertex: " + network.getId());
            graph.addVertex(network);
        }
        // Add SDP as edges.
        NsiTopology nsiTopology = provider.getTopology();
        for (SdpType sdp : provider.getTopology().getSdps()) {
            System.out.println("Adding Edge: " + sdp.getId());
            
            StpType stpA = nsiTopology.getStp(sdp.getStpA().getId());
            StpType stpZ = nsiTopology.getStp(sdp.getStpZ().getId());
            
            graph.addEdge(sdp,
                    provider.getTopology().getNetworkById(stpA.getNetworkId()),
                    provider.getTopology().getNetworkById(stpZ.getNetworkId()));
        }
        
        NetworkType a = provider.getTopology().getNetworkById("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
        NetworkType z = provider.getTopology().getNetworkById("urn:ogf:network:kddilabs.jp:2013:topology");
        
        DijkstraShortestPath<NetworkType,SdpType> alg = new DijkstraShortestPath<>(graph);
        List<SdpType> list = alg.getPath(a, z);

        System.out.println("The shortest unweighted path from " + a + " to " + z + " is:");
        for (SdpType sdp : list) {
            System.out.println("  " + sdp.getId());
        }
    }
}
