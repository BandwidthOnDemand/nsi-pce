/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.test;

import java.io.File;
import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author hacksaw
 */
public class TestConfig {
    private static final String configDir = "src/test/resources/config/";
    private static final String log4jConfig = new StringBuilder(configDir).append("log4j.xml").toString().replace("/", File.separator);
    private static final String beanConfig = new StringBuilder(configDir).append("beans.xml").toString().replace("/", File.separator);
        
    private TopologyProvider provider;

    private TestConfig() {
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);
        
        // Get a reference to the topology provider through spring.
        SpringContext sc = SpringContext.getInstance();
        ApplicationContext context = sc.initContext(beanConfig);
        provider = (TopologyProvider) context.getBean("topologyProvider");
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
