package net.es.nsi.pce.server;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.config.StartupArgs;
import net.es.nsi.pce.util.LogbackConfigurator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * This is the main execution thread for the PAth Computation Engine.  The
 * runtime configuration is loaded from disk, the HTTP server is started, and
 * a shutdown hook is added to monitor for termination conditions and clean up
 * services.
 *
 * @author hacksaw
 */
public class Main {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    public static final String PCE_SERVER_CONFIG_NAME = "pce";

    // Keep running PCE while true.
    private static boolean keepRunning = true;

    /**
     * Returns a boolean indicating whether the PCE should continue running
     * (true) or should terminate (false).
     *
     * @return true if the PCE should be running, false otherwise.
     */
    public static boolean isKeepRunning() {
        return keepRunning;
    }

    /**
     * Set whether the PCE should be running (true) or terminated (false).
     *
     * @param keepRunning true if the PCE should be running, false otherwise.
     */
    public static void setKeepRunning(boolean keepRunning) {
        Main.keepRunning = keepRunning;
    }

    /**
     * Main initialization and execution thread for the NSI Path Computation
     * Engine.  Method will loop until a signal is received to shutdown.
     *
     * @param args No arguments are accepted.
     * @throws Exception If anything fails during initialization.
     */
    public static void main(String[] args) throws Exception {
      LogManager.getLogManager().reset();
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
      Logger.getLogger("global").setLevel(Level.FINEST);

        CommandLine cmd = null;
        try {
            cmd = StartupArgs.parse(args);
        } catch (ParseException e) {
            System.err.println("You did not provide the correct arguments, see usage below.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar pce.jar  [-c <configDir>] [-topologyConfigFile <filename>]", StartupArgs.options());
            System.exit(1);
            return;
        }

        String configPath = StartupArgs.apply(cmd);

        // Load the logging configuration; logback.xml sets its own scan interval.
        LogbackConfigurator.configure(configPath, "logback.xml");

        log.info("Starting nsi-pce {} on Java {}",
                Main.class.getPackage().getImplementationVersion(),
                System.getProperty("java.version"));

        // Load PCE configuration from disk.
        ConfigurationManager.INSTANCE.initialize(configPath);

        // Listen for a shutdown event so we can clean up.
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                @Override
                public void run() {
                    log.info("Shutting down PCE...");
                    PCEServer.getInstance().shutdown();
                    log.info("...Shutdown complete.");
                    Main.setKeepRunning(false);
                }
            }
        );

        // Loop until we are told to shutdown.
        while (keepRunning) {
            Thread.sleep(1000);
        }
    }
}
