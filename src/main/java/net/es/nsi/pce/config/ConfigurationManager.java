package net.es.nsi.pce.config;

import net.es.nsi.pce.spring.SpringContext;
import java.io.File;
import net.es.nsi.pce.discovery.provider.DiscoveryProvider;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import net.es.nsi.pce.sched.PCEScheduler;
import net.es.nsi.pce.sched.TopologyAudit;
import net.es.nsi.pce.server.PCEServer;
import org.apache.log4j.xml.DOMConfigurator;
import org.quartz.SchedulerException;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * The Path Computation Engine's Configuration Manager loads initial
 * configuration files and instantiates singletons.  Spring Beans are used
 * to drive initialization and created singletons for the key services.
 *
 * HttpConfigProvider - This provider contains configuration for the PCEServer
 * and AggServer.  The PCEServer drives the core logic for path computation and
 * exposes the web services interfaces as an external API.  The AggServer is a
 * test endpoint for receiving PCE results and is not utilized during normal
 * operation.
 *
 * ServiceInfoProvider - This provider loads NSA configuration and security
 * information from a configuration file.  This information is is not used
 * during pat computation, but is exposed through a web service interface for
 * use by the aggregator for peer communications.  ** This functionality should
 * not be part of the PCE and will be moved to a Discovery Service at a later
 * date. **
 *
 * TopologyProvider - This provider loads network topology information used
 * during the path computation process.  Topology is currently loaded from a
 * local configuration file but will be changed in the near future to use an
 * external topology service.
 *
 * PCEScheduler - The scheduling task for PCE functions such as configuration
 * monitoring and other maintenance tasks.
 *
 * @author hacksaw
 */
public enum ConfigurationManager {
    INSTANCE;

    private static PCEServer pceServer;
    private static TopologyProvider topologyProvider;
    private static DiscoveryProvider discoveryProvider;
    private static PCEScheduler pceScheduler;
    private static ApplicationContext context;

    private boolean initialized = false;

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @param aInitialized the initialized to set
     */
    public void setInitialized(boolean aInitialized) {
        initialized = aInitialized;
    }

    /**
     * This method initialized the PCE configuration found under the specified
     * configPath.
     *
     * @param configPath The path containing all the needed configuration files.
     * @throws Exception If there is an error loading any of the required configuration files.
     */
    public synchronized void initialize(String configPath) throws Exception {
        if (!isInitialized()) {
            // Build paths for configuration files.
            String log4jConfig = new StringBuilder(configPath).append("log4j.xml").toString();
            String beanConfig = new StringBuilder(configPath).append("beans.xml").toString();

            // Load and watch the log4j configuration file for changes.
            DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);

            final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigurationManager.class);

            // Initialize the Spring context to load our dependencies.
            SpringContext sc = SpringContext.getInstance();
            context = sc.initContext(beanConfig);

            // Get references to the spring controlled beans.
            pceServer = (PCEServer) context.getBean("pceServer");
            pceServer.start();

            // Start the discovery process.
            discoveryProvider = (DiscoveryProvider) context.getBean("discoveryProvider");
            discoveryProvider.start();

            // Start the NIS network topology build.
            topologyProvider = (TopologyProvider) context.getBean("topologyProvider");
            try {
                topologyProvider.loadTopology();
            }
            catch (Exception ex) {
                log.error("Failed to load topology.", ex);
            }

            pceScheduler = (PCEScheduler) context.getBean("pceScheduler");
            pceScheduler.add(TopologyAudit.JOBNAME, TopologyAudit.JOBGROUP, TopologyAudit.class, getTopologyProvider().getAuditInterval()*1000);
            pceScheduler.start();

            setInitialized(true);
            log.info("Loaded configuration from: " + configPath);
        }
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * @return the pceServer
     */
    public PCEServer getPceServer() {
        return pceServer;
    }

    /**
     * @return the topology provider
     */
    public TopologyProvider getTopologyProvider() {
        return topologyProvider;
    }

    /**
     * @return the discoveryProvider
     */
    public DiscoveryProvider getDiscoveryProvider() {
        return discoveryProvider;
    }

    public void shutdown() {
        try {
            discoveryProvider.shutdown();
            pceServer.stop();
            pceScheduler.stop();
            initialized = false;
        }
        catch (IllegalStateException | SchedulerException ex) {
        }
    }
}
