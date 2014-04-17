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
    private final static String CONFIG_DIR = "src/test/resources/config/";
    private static final String DEFAULT_TOPOLOGY_FILE = CONFIG_DIR + "topology-dds.xml";
    private static final String DEFAULT_DDS_FILE = CONFIG_DIR + "dds.xml";
    private static final String TOPOLOGY_CONFIG_FILE_ARGNAME = "topologyConfigFile";
    private static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";
        
    private TopologyProvider provider;

    private TestConfig() {
        System.setProperty(DDS_CONFIG_FILE_ARGNAME, DEFAULT_DDS_FILE);
        System.setProperty(TOPOLOGY_CONFIG_FILE_ARGNAME, DEFAULT_TOPOLOGY_FILE);
        try {
            ConfigurationManager.INSTANCE.initialize(CONFIG_DIR);
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
