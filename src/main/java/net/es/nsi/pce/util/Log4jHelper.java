/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.es.nsi.pce.server.Properties;

/**
 *
 * @author hacksaw
 */
public class Log4jHelper {
    private static final String DEFAULT_LOGF4J = "config/log4j.xml";

    /**
     * Get the Log4J configuration file.
     *
     * @param file File containing the Log4J configuration.
     * @return Log4J configuration file.
     * @throws IOException If path to configuration files is invalid.
     */
    public static String getLog4jConfig(String file) throws IOException {
        Path realPath;

        // Check the log4j system property first.
        String log4jConfig = System.getProperty(Properties.SYSTEM_PROPERTY_LOG4J, file);
        if (log4jConfig == null) {
            // No system property so we take a guess based on local information.
            realPath = Paths.get(DEFAULT_LOGF4J).toRealPath();
        }
        else {
            realPath = Paths.get(log4jConfig).toRealPath();
        }

        return realPath.toString();
    }
}
