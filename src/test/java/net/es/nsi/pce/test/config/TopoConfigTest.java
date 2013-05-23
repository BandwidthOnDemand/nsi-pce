package net.es.nsi.pce.test.config;

import net.es.nsi.pce.topo.JsonTopoConfigProvider;
import org.testng.annotations.Test;

public class TopoConfigTest {
    @Test (groups = "config")
    public void testTopoLoad() throws Exception {
        System.out.println("testing topology config");


        JsonTopoConfigProvider prov = JsonTopoConfigProvider.getInstance();
        prov.setFilename("src/test/resources/config/topo.json");
        prov.loadConfig();

        for (String networkId: prov.getNetworkIds()) {
            System.out.println("loaded config for network "+networkId);

        }
    }

}
