package net.es.nsi.pce.config;

import java.io.File;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.config.http.HttpConfigProvider;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.discovery.provider.DiscoveryProvider;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import net.es.nsi.pce.sched.PCEScheduler;
import net.es.nsi.pce.sched.TopologyAudit;
import org.apache.log4j.xml.DOMConfigurator;
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

    private static HttpConfigProvider htProv;
    private static HttpConfig pceConfig;    
    private static ServiceInfoProvider sip;
    private static TopologyProvider topologyProvider;
    private static DiscoveryProvider discoveryProvider;
    
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
            ApplicationContext context = sc.initContext(beanConfig);

            // Load the HTTP provider configuration bean.
            setHttpProv((HttpConfigProvider) context.getBean("httpConfigProvider"));

            // Load and start the Path Computation Engine HTTP server.
            log.info("Loading PCE HTTP config...");
            setPceConfig(getHttpProv().getConfig("pce"));
            log.info("...PCE HTTP config loaded.");

            // Load the NSA addressing and security information.
            log.info("Loading NSA security config...");
            setServiceInfoProvider((ServiceInfoProvider) context.getBean("serviceInfoProvider"));
            log.info("...NSA security config loaded.");
            
            // Load discovery provider.
            log.info("Loading discovery provider...");
            setDiscoveryProvider((DiscoveryProvider) context.getBean("discoveryProvider"));
            log.info("...Discovery provider loaded.");

            // Load topology database.
            log.info("Loading topology provider...");
            setTopologyProvider((TopologyProvider) context.getBean("topologyProvider"));
            log.info("...Topology provider loaded.");
            
            // TODO: This need to change to a local cache load.
            log.info("Loading network topology...");
            getTopologyProvider().loadTopology();
            log.info("...network topology loaded.");
            
            // Start the task scheduler.
            log.info("Starting task scheduler...");
            log.info("--- Adding topology audit for " + getTopologyProvider().getAuditInterval());
            PCEScheduler.getInstance().add(TopologyAudit.JOBNAME, TopologyAudit.JOBGROUP, TopologyAudit.class, getTopologyProvider().getAuditInterval());
            PCEScheduler.getInstance().start();
            log.info("...Task scheduler started.");

            initialized = true;
            log.info("Loaded configuration from: " + configPath);
        }
    }

    /**
     * @return the htProv
     */
    public HttpConfigProvider getHttpProv() {
        return htProv;
    }

    /**
     * @param aHtProv the htProv to set
     */
    public void setHttpProv(HttpConfigProvider aHtProv) {
        htProv = aHtProv;
    }

    /**
     * @param aPceConfig the pceConfig to set
     */
    public void setPceConfig(HttpConfig aPceConfig) {
        pceConfig = aPceConfig;
    }
    
    public HttpConfig getPceConfig() {
        if (pceConfig == null) {
            throw new IllegalStateException();
        }
        
        return pceConfig;
    }

    /**
     * @return the sip
     */
    public ServiceInfoProvider getServiceInfoProvider() {
        return sip;
    }

    /**
     * @param aSip the sip to set
     */
    public void setServiceInfoProvider(ServiceInfoProvider aSip) {
        sip = aSip;
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
        discoveryProvider.shutdown();
    }
}
