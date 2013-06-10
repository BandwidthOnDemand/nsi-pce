package net.es.nsi.pce.test.config;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.pf.api.topo.*;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class TopoConfigTest {
    private ApplicationContext context;

    @BeforeSuite(groups = {"config", "spring"})
    public void loadSpring() {
        SpringContext sc = SpringContext.getInstance();
        context = sc.initContext("src/test/resources/config/beans.xml");
    }

    @Test (groups = {"config", "spring"})
    public void testTopoLoad() throws Exception {
        System.out.println("testing topology config");


        TopologyProvider prov = (TopologyProvider) context.getBean("topologyProvider");
        prov.loadTopology();

        for (String networkId: prov.getNetworkIds()) {
            System.out.println("loaded config for Network "+networkId);
        }

        Topology topo = prov.getTopology();
        for (String netId : topo.getNetworkIds()) {
            System.out.println(netId);
            Network net = topo.getNetwork(netId);
            for (String stpId : net.getStpIds()) {
                Stp stp = net.getStp(stpId);

                if (net.getConnectionsFrom(stp).isEmpty()) {
                    System.out.println("  "+stp.getLocalId());
                } else {
                    for (StpConnection conn : net.getConnectionsFrom(stp)) {
                        System.out.println("  "+stp.getLocalId()+" -- "+conn.getZ().getLocalId());

                    }
                }
            }
        }


    }

}
