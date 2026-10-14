package net.es.nsi.pce.config;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The command line shared by the PCE and the TopologyViewer: -c for the configuration directory and
 * -topologyConfigFile, published as the configPath and topologyConfigFile system properties that
 * beans.xml reads.
 */
public final class StartupArgs {
    public static final String CONFIG_PATH = "configPath";
    public static final String TOPOLOGY_CONFIG_FILE_ARGNAME = "topologyConfigFile";
    private static final String CONFIG_DEFAULT_PATH = "config/";
    private static final String DEFAULT_TOPOLOGY_FILE = CONFIG_DEFAULT_PATH + "topology-dds.xml";

    private StartupArgs() {
    }

    public static Options options() {
        Options options = new Options();

        Option topologyOption = new Option(TOPOLOGY_CONFIG_FILE_ARGNAME, true, "Path to your topology configuration file");
        topologyOption.setRequired(false);
        options.addOption(topologyOption);

        Option configOption = new Option("c", true, "Where you keep your other configfiles (defaults to ./config)");
        configOption.setRequired(false);
        options.addOption(configOption);
        return options;
    }

    public static CommandLine parse(String[] args) throws ParseException {
        // GnuParser, not DefaultParser: only it accepts -topologyConfigFile=<file>, the form the Helm chart passes.
        @SuppressWarnings("deprecation")
        CommandLineParser parser = new GnuParser();
        return parser.parse(options(), args);
    }

    /**
     * Sets the configPath and topologyConfigFile system properties from the command line, with their
     * defaults.
     *
     * @return the configuration directory, ending in a slash.
     */
    public static String apply(CommandLine cmd) {
        String configPath = cmd.getOptionValue("c");
        if (configPath == null) {
            configPath = CONFIG_DEFAULT_PATH;
        } else {
            configPath = configPath.trim();
            if (!configPath.endsWith("/")) {
                configPath = configPath + "/";
            }
        }
        System.setProperty(CONFIG_PATH, configPath);

        String topologyFile = cmd.getOptionValue(TOPOLOGY_CONFIG_FILE_ARGNAME);
        if (topologyFile == null || topologyFile.isEmpty()) {
            topologyFile = DEFAULT_TOPOLOGY_FILE;
        }
        System.setProperty(TOPOLOGY_CONFIG_FILE_ARGNAME, topologyFile);

        return configPath;
    }
}
