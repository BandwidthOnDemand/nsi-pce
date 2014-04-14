/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.test;

import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.topology.provider.TopologyProvider;

/**
 *
 * @author hacksaw
 */
public class TestConfig {
    private static final String configDir = "src/test/resources/config/";
        
    private TopologyProvider provider;

    private TestConfig() {
        try {
            ConfigurationManager.INSTANCE.initialize(configDir);
        }
        catch (Exception ex) {
            System.err.println("TestConfig: failed to initialize ConfigurationManager.");
            ex.printStackTrace();
        }
    }

    private static class TestConfigHolder {
        public static final TestConfig INSTANCE = new TestConfig();
    }

    public static TestConfig getInstance() {
            return TestConfigHolder.INSTANCE;
    }
    
    public TopologyProvider getTopologyProvider() {
        return provider;
    }
    
}
