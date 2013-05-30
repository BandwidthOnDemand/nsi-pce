package net.es.nsi.pce.test.pf;

import net.es.nsi.pce.config.topo.JsonTopoConfigProvider;
import net.es.nsi.pce.pf.DijkstraPCE;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.cons.PathEndpoints;
import net.es.nsi.pce.pf.api.topo.JsonTopologyProvider;
import net.es.nsi.pce.pf.api.topo.Topology;
import org.testng.annotations.Test;

public class DijkstraTest {
    @Test(groups = "pf")
    public void testDijkstra() throws Exception {
        JsonTopoConfigProvider prov = JsonTopoConfigProvider.getInstance();
        prov.setFilename("src/test/resources/config/topo.json");
        JsonTopologyProvider jtp = JsonTopologyProvider.getInstance();

        Topology topo = jtp.getTopology();

        PCEData pceData = new PCEData();

        pceData.setTopo(topo);


        PathEndpoints pe = new PathEndpoints();
        pe.setSrcLocal("surfnet-edge");
        pe.setSrcNetwork("urn:ogf:network:stp:surfnet.nl");
        pe.setDstLocal("esnet-edge-one");
        pe.setDstNetwork("urn:ogf:network:stp:es.net");
        pceData.getConstraints().add(pe);



        DijkstraPCE pce = new DijkstraPCE();
        pce.apply(pceData);

    }
}
