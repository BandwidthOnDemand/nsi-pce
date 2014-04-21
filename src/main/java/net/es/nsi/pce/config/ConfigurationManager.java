package net.es.nsi.pce.config;

import net.es.nsi.pce.spring.SpringContext;
import java.io.File;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.config.http.HttpConfigProvider;
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

    public static final String PCE_SERVER_CONFIG_NAME = "pce";
    
    private static HttpConfigProvider httpConfigProvider;
    private static PCEServer pceServer;
    private static TopologyProvider topologyProvider;
    private static DiscoveryProvider discoveryProvider;
    
    ApplicationContext context;
    
    private static boolean initialized = false;
    
    /**
     * This method initialized the PCE configuration found under the specified
     * configPath.
     * 
     * @param configPath The path containing all the needed configuration files.
     * @throws Exception If there is an error loading any of the required configuration files.
     */
    public synchronized void initialize(String configPath) throws Exception {
        if (!initialized) {
            // Build paths for configuration files.
            String log4jConfig = new StringBuilder(configPath).append("log4j.xml").toString().replace("/", File.separator);
            String beanConfig = new StringBuilder(configPath).append("beans.xml").toString().replace("/", File.separator);

            // Load and watch the log4j configuration file for changes.
            DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);

            final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigurationManager.class);

            // Initialize the Spring context to load our dependencies.
            SpringContext sc = SpringContext.getInstance();
            
            log.info("Loading Spring context...");
            context = sc.initContext(beanConfig);
            log.info("Spring context loaded.");
            
            // Get references to the spring controlled beans.
            httpConfigProvider = (HttpConfigProvider) context.getBean("httpConfigProvider");
            pceServer = (PCEServer) context.getBean("pceServer");
            pceServer.start(PCE_SERVER_CONFIG_NAME);
            
            // Start the discovery process.
            discoveryProvider = (DiscoveryProvider) context.getBean("discoveryProvider");
            discoveryProvider.start();
            
            //serviceInfoProvider = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
            topologyProvider = (TopologyProvider) context.getBean("topologyProvider");

            // TODO: This need to change to a local cache load.
            log.info("Loading network topology...");
            getTopologyProvider().loadTopology();
            log.info("...network topology loaded.");
            
            // Start the task scheduler.
            log.info("Starting task scheduler...");
            log.info("--- Adding topology audit for " + getTopologyProvider().getAuditInterval());
            PCEScheduler.getInstance().add(TopologyAudit.JOBNAME, TopologyAudit.JOBGROUP, TopologyAudit.class, getTopologyProvider().getAuditInterval()*1000);
            PCEScheduler.getInstance().start();
            log.info("...Task scheduler started.");

            initialized = true;
            log.info("Loaded configuration from: " + configPath);
        }
    }
    
    public ApplicationContext getApplicationContext() {
        return context;
    }
    
    public HttpConfig getPceConfig() {
        if (httpConfigProvider == null) {
            throw new IllegalStateException();
        }
        return httpConfigProvider.getConfig(PCE_SERVER_CONFIG_NAME);
    }

    /**
     * @return the pceServer
     */
    public static PCEServer getPceServer() {
        return pceServer;
    }

    /**
     * @param aPceServer the pceServer to set
     */
    public static void setPceServer(PCEServer aPceServer) {
        pceServer = aPceServer;
    }

    /**
     * @return the topology provider
     */
    public TopologyProvider getTopologyProvider() {
        return topologyProvider;
    }

    /**
     * @param aTp the topology provider to set
     */
    public void setTopologyProvider(TopologyProvider aTp) {
        topologyProvider = aTp;
    }
    

    /**
     * @return the discoveryProvider
     */
    public DiscoveryProvider getDiscoveryProvider() {
        return discoveryProvider;
    }

    /**
     * @param aDiscoveryProvider the discoveryProvider to set
     */
    public void setDiscoveryProvider(DiscoveryProvider aDiscoveryProvider) {
        discoveryProvider = aDiscoveryProvider;
    }
    
    public void shutdown() {
        
        try {
            discoveryProvider.shutdown();
            pceServer.stop();
            PCEScheduler.getInstance().stop();
        }
        catch (IllegalStateException | SchedulerException ex) {
        }
    }
}
