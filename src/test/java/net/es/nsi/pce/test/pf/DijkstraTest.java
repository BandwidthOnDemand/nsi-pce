package net.es.nsi.pce.test.pf;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.pf.DijkstraPCE;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class DijkstraTest {
    private ApplicationContext context;


    @BeforeSuite(groups = {"pf", "dijkstra"})
    public void loadSpring() {
        SpringContext sc = SpringContext.getInstance();
        context = sc.initContext("src/test/resources/config/beans.xml");
    }

    @Test(groups = {"pf", "dijkstra"})
    public void testDijkstra() throws Exception {
        TopologyProvider prov = (TopologyProvider) context.getBean("topologyProvider");
        prov.loadTopology();

        Topology topo = prov.getTopology();

        PCEData pceData = new PCEData();

        pceData.setTopo(topo);


        TopoPathEndpoints pe = new TopoPathEndpoints();
        pe.setSrcLocal("surfnet-edge");
        pe.setSrcNetwork("urn:ogf:network:stp:surfnet.nl");
        pe.setDstLocal("esnet-edge-one");
        pe.setDstNetwork("urn:ogf:network:stp:es.net");
        pceData.getConstraints().add(pe);

        DijkstraPCE pce = new DijkstraPCE();
        pce.apply(pceData);

    }

    @Test(groups = "pf")
    public void testEntry() throws Exception {
        TopologyProvider prov = (TopologyProvider) context.getBean("topologyProvider");
        prov.loadTopology();

        Topology topo = prov.getTopology();

        PCEData pceData = new PCEData();

        pceData.setTopo(topo);


        TopoPathEndpoints pe = new TopoPathEndpoints();
        pe.setSrcLocal("surfnet-edge");
        pe.setSrcNetwork("urn:ogf:network:stp:surfnet.nl");
        pe.setDstLocal("esnet-edge-one");
        pe.setDstNetwork("urn:ogf:network:stp:es.net");
        pceData.getConstraints().add(pe);

        PCEModule pce = (PCEModule) context.getBean("chainPCE");
        pce.apply(pceData);

    }

}
