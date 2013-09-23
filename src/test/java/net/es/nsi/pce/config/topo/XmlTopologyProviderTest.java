/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.test.TestConfig;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 *
 * @author hacksaw
 */
public class XmlTopologyProviderTest {
    @Test
    public void test() {
        TestConfig.loadConfig();
                
        XmlTopologyProvider provider = new XmlTopologyProvider();
        
        provider.setTopologySource("config/topology/");
        
        try {
            provider.loadTopology();
        }
        catch (Exception ex) {
            System.err.println("XmlTopologyProviderTest.loadTopology() Failed: ");
            ex.printStackTrace();
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
                System.err.println("XmlTopologyProviderTest: dump of topology failed: ");
                ex.printStackTrace();
                fail();
            }
        }
    }
}
