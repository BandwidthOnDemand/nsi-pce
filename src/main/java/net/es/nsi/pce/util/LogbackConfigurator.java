package net.es.nsi.pce.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter2;
import java.io.File;
import org.slf4j.LoggerFactory;

/**
 * Loads a Logback configuration file from the -c configuration directory, which is only known after
 * Logback has initialised. Does nothing when -Dlogback.configurationFile is set or the file is absent.
 */
public final class LogbackConfigurator {
    private LogbackConfigurator() {
    }

    public static void configure(String configDir, String filename) {
        File file = new File(configDir, filename);
        if (System.getProperty("logback.configurationFile") != null || !file.isFile()) {
            return;
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(file);
        } catch (JoranException ex) {
            // Reported below together with any other configuration problem.
        }
        new StatusPrinter2().printInCaseOfErrorsOrWarnings(context);
    }
}
