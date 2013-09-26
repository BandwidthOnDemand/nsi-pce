/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.test;

import java.io.File;
import org.apache.log4j.xml.DOMConfigurator;

/**
 *
 * @author hacksaw
 */
public class TestConfig {
    public static void loadConfig() {
        // Build paths for configuration files.
        String log4jConfig = new StringBuilder("src/test/resources/config/").append("log4j.xml").toString().replace("/", File.separator);
        
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);        
    }
}
