package net.es.nsi.pce.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import net.es.nsi.pce.config.ConfigurationManager;

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
    
    private static final String CONFIG_DEFAULT_PATH = "config/";
    public static final String TOPOLOGY_CONFIG_FILE_ARGNAME = "topologyConfigFile";
    public static final String LOCAL_NETWORK_ID = "localNetworkId";
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
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        // Create Options object to hold our command line options.
        Options options = new Options();

        // Configuration directory option.
        Option topologyOption = new Option(TOPOLOGY_CONFIG_FILE_ARGNAME, true, "Path to your topology configuration file");
        topologyOption.setRequired(true);
        options.addOption(topologyOption);

        Option localNetworkOption = new Option(LOCAL_NETWORK_ID, true, "The id of your local network, e.g: urn:ogf:network:surfnet.nl:1990:testbed");
        localNetworkOption.setRequired(false);
        options.addOption(localNetworkOption);

        options.addOption("c", true, "Where you keep your other configfiles (defaults to ./config)");
        
        // Parse the command line options.
        CommandLineParser parser = new GnuParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("You did not provide the correct arguments, see usage below.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar pce.jar  -c [configDir] -topologyConfigFile [file] [-localNetworkId=[NSI network Id]].\n Setting the localNetworkId implies that you want to run in Gang Of 3 (reachability-pce) mode", options );
            System.exit(1);
        }

        // Look for our options.
        String configPath = cmd.getOptionValue("c");
        if(configPath == null) {
            configPath = CONFIG_DEFAULT_PATH;
        }

        System.setProperty("topologyProviderConfigPath", cmd.getOptionValue(TOPOLOGY_CONFIG_FILE_ARGNAME));

        if (cmd.getOptionValue(LOCAL_NETWORK_ID) != null) {
            System.setProperty(LOCAL_NETWORK_ID, cmd.getOptionValue(LOCAL_NETWORK_ID));
        }
        // Load PCE configuration from disk.
        ConfigurationManager.INSTANCE.initialize(configPath);

        // Listen for a shutdown event so we can clean up.
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                @Override
                public void run() {
                    log.info("Shutting down PCE...");
                    PCEServer.getInstance().stop();
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
