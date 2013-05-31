package net.es.nsi.pce.test.config;

import net.es.nsi.pce.config.topo.JsonTopoConfigProvider;
import net.es.nsi.pce.pf.api.topo.JsonTopologyProvider;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;
import org.testng.annotations.Test;

public class TopoConfigTest {
    @Test (groups = "config")
    public void testTopoLoad() throws Exception {
        System.out.println("testing topology config");


        JsonTopoConfigProvider prov = JsonTopoConfigProvider.getInstance();
        prov.setFilename("src/test/resources/config/topo.json");
        try {
            prov.loadConfig();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        for (String networkId: prov.getNetworkIds()) {
            System.out.println("loaded config for Network "+networkId);
        }

        JsonTopologyProvider jtp = JsonTopologyProvider.getInstance();
        Topology topo = jtp.getTopology();
        for (String netId : topo.getNetworkIds()) {
            System.out.println(netId);
            for (String stpId : topo.getNetwork(netId).getStpIds()) {
                Stp stp = topo.getNetwork(netId).getStp(stpId);
                if (stp.remote == null) {
                    System.out.println("  "+stp.localId);
                } else {
                    System.out.println("  "+stp.localId+" -- "+stp.remote.localId);
                }
            }
        }


    }

}
