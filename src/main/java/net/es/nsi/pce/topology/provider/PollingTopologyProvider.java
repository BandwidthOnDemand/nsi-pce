package net.es.nsi.pce.topology.provider;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.config.topo.nml.TopologyManifest;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.config.jaxb.ConfigurationType;
import net.es.nsi.pce.schema.TopologyConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.model.NsiSdpFactory;


/**
 * Dynamically download the NSI/NML topology from peer NSA, parse, and
 * consolidate into a connected network topology.  The list of NSA offering
 * dynamic download of topology is specified in the XML bootstrap document
 * loaded from the location specified in the TopologyProvider configuration
 * file.
 * 
 * @author hacksaw
 */
@Component
public class PollingTopologyProvider implements TopologyProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
      
    // Location of configuration file.
    private String configuration;
        
    // Our topology manifest provider.
    private TopologyManifestReader topologyManifestReader;
    
     // Our toplogy reader factory.
    private TopologyReaderFactory  topologyReaderFactory;   
    
    // topology manifest enpoint.
    private String location = null;
    
    // Time between topology refreshes.
    private long auditInterval = 30*60*1000;  // Default 30 minute polling time.
    
    // Time of last audit.
    private long lastAudit = 0;
    
    // Default serviceType provided by topology.
    private String defaultServiceType = "http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE";

    // Our topology manifest.
    private TopologyManifest topologyManifest;
    
    // NSA topology entries indexed by string URL.
    private Map<String, NmlTopologyReader> topologyUrlMap = new ConcurrentHashMap<>();
    
    // The NSI Topology model used by path finding.
    private NsiTopology nsiTopology = new NsiTopology();
    
    // The last time an NML object was discovered.
    private long lastModified = 0L;
    
    // Overall discovery status. 
    private TopologyStatus summaryStatus = TopologyStatus.Initializing;
    
    // The topology manifest discovery status. 
    private TopologyProviderStatus manifestStatus = null;

    // The topology provider discovery status. 
    private Map<String, TopologyProviderStatus> providerStatus = new ConcurrentHashMap<>();
    
    /**
     * Default class constructor.
     */
    public PollingTopologyProvider() {}
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public PollingTopologyProvider(String configFile) {
        this.configuration = configFile;
    }

    /**
     * @return the remoteEndpoint
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param remoteEndpoint the remoteEndpoint to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the auditInterval
     */
    @Override
    public long getAuditInterval() {
        return auditInterval;
    }

    /**
     * @param auditInterval the auditInterval to set
     */
    public void setAuditInterval(long auditInterval) {
        this.auditInterval = auditInterval;
    }

    /**
     * @return the defaultServiceType
     */
    public String getDefaultServiceType() {
        return defaultServiceType;
    }

    /**
     * @param defaultServiceType the defaultServiceType to set
     */
    public void setDefaultServiceType(String defaultServiceType) {
        this.defaultServiceType = defaultServiceType;
    }
    
    private void loadTopologyConfiguration() throws JAXBException, FileNotFoundException {
        ConfigurationType config = TopologyConfigurationParser.getInstance().parse(configuration);
        
        setLocation(config.getLocation());
        
        if (config.getAuditInterval() > 0) {
            setAuditInterval(config.getAuditInterval());
        }
        
        if (config.getDefaultServiceType() != null &&
                !config.getDefaultServiceType().isEmpty()) {
            setDefaultServiceType(config.getDefaultServiceType());
        }
        
        topologyManifestReader.setTarget(getLocation());
    }

    private void loadNetworkTopology() throws Exception {
        loadTopologyConfiguration();
        
        // Identify that we have started an audit.
        manifestAuditStart();
        
        // Get an updated copy of the topology manifest file.
        TopologyManifest newManifest;
        try {
            newManifest = getTopologyManifestReader().getManifestIfModified();
        }
        catch (Exception ex) {
            // Identify that we have failed the audit.
            manifestAuditError();
            log.error("loadNetworkTopology: Failed to load topology manifest " + getTopologyManifestReader().getTarget(), ex);
            throw ex;
        }
        
        // See if the topology manifest list has changed.
        if (newManifest != null) {
            topologyManifest = newManifest;
        }
        
        // Check to see if we have a valid topology manifest.
        if (topologyManifest == null) {
            // Identify that we have failed the audit.
            manifestAuditError();
            log.error("loadNetworkTopology: Failed to load topology manifest " + getTopologyManifestReader().getTarget());
            throw new NotFoundException("Failed to load topology manifest " + getTopologyManifestReader().getTarget());               
        }
        
        // Identify s successful audit.
        manifestAuditSuccess();
        
        // Create a new topology URL map.
        Map<String, NmlTopologyReader> newTopologyUrlMap = new ConcurrentHashMap<>();
        Map<String, TopologyProviderStatus> newProviderStatus = new ConcurrentHashMap<>();
        // Load each topology from supplied endpoint.  If we fail to load a new
        // topology, then keep the old one for now.
        Map<String, String> entryList = topologyManifest.getEntryList();
        for (Map.Entry<String, String> entry : entryList.entrySet()) {
            String id = entry.getKey();
            String href = entry.getValue();
            
            // Check to see if we have already discovered this NSA.
            NmlTopologyReader originalReader = topologyUrlMap.get(id);
            NmlTopologyReader reader;
            if (originalReader == null) {
                // We not have one so create a new reader.
                reader = topologyReaderFactory.getReader(id, href);
                reader.setDefaultServiceType(defaultServiceType);
            }
            else {
                reader = originalReader;
            }
            
            // Identify this provider audit as starting.
            TopologyProviderStatus auditStatus = providerAuditStart(reader, newProviderStatus);
            
            // Attempt to discover this topology.
            try {
                reader.load();
            }
            catch (Exception ex) {
                // If we have already read this topology successfully, then
                // keep it on failure.
                providerAuditError(auditStatus);
                
                log.error("loadTopology: Failed to load NSA topology, id=" + id + ", href=" + href, ex);
                if (originalReader == null) {
                    // We have never successfully discovered this NSA so skip.
                    continue;
                }
            }
            
            newTopologyUrlMap.put(id, reader);
            updateLastModified(reader.getLastDiscovered());
            providerAuditSuccess(reader, auditStatus);
        }

        // We are done so update the map with the new view.
        topologyUrlMap = newTopologyUrlMap;
        providerStatus = newProviderStatus;
        
        // Consolidate individual topologies in one master list.
        NsiTopology newTopology = new NsiTopology();
        for (NmlTopologyReader nml : topologyUrlMap.values()) {
            newTopology.add(nml.getNsiTopology());
        }
        
        newTopology = consolidateGlobalTopology(newTopology);
        
        // Update the gloabl topology with the new view.
        newTopology.setLastModified(getLastModified());
        nsiTopology = newTopology;
    }
    
    private NsiTopology consolidateGlobalTopology(NsiTopology topology) {
        topology.addAllSdp(NsiSdpFactory.createUnidirectionalSdpTopology(topology.getStpMap()));
        topology.addAllSdp(NsiSdpFactory.createBidirectionalSdps(topology.getStpMap()));
        return topology;
    }

    /**
     * Take the NML topology for the set of interconnected networks and compute
     * the NSI related topology objects (NSA, Network, Adaption, STP, and SDP)
     * used for path calculations.
     * 
     * @throws Exception If there are invalid topologies.
     */
    @Override
    public synchronized void loadTopology() throws Exception {   
        summaryAuditStart();
        
        // Load the NML topology model.
        try {
            loadNetworkTopology();
        }
        catch (Exception ex) {
            summaryAuditError();
            log.error("loadNetworkTopology failed, setting provider status to Error", ex);
            throw ex;
        }
        
        // We completed a topology audit so mark it as such.
        summaryAuditSuccess();
    }
    
    @Override
    public NsiTopology getTopology() {
        return nsiTopology;
    }
    
    public Set<String> getNetworkIds() {
        return nsiTopology.getNetworkIds();
    }

    public Collection<NetworkType> getNetworks() {
        return nsiTopology.getNetworks();
    }
    
    /**
     * For this provider we read the topology source from an XML configuration
     * file.
     * 
     * @param source Path to the XML configuration file.
     */
    @Override
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the source file of the topology configuration.
     * 
     * @return The path to the XML configuration file. 
     */
    @Override
    public String getConfiguration() {
        return configuration;
    }
    
     /**
     * For this provider we read the topology source from an XML configuration
     * file.
     * 
     * @param source Path to the XML configuration file.
     */
    @Override
    public void initialize() throws Exception {
        this.loadTopologyConfiguration();
    }   

    /**
     * @return the topologyManifestReader
     */
    public TopologyManifestReader getTopologyManifestReader() {
        return topologyManifestReader;
    }

    /**
     * @param topologyManifestReader the topologyManifestReader to set
     */
    public void setTopologyManifestReader(TopologyManifestReader manifestReader) {
        this.topologyManifestReader = manifestReader;
    }

    /**
     * @return the topologyReaderFactory
     */
    public TopologyReaderFactory getTopologyReaderFactory() {
        return topologyReaderFactory;
    }

    /**
     * @param topologyReaderFactory the topologyReaderFactory to set
     */
    public void setTopologyReaderFactory(TopologyReaderFactory topologyReaderFactory) {
        this.topologyReaderFactory = topologyReaderFactory;
    }

    @Override
    public void loadCache() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void saveCache() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the lastModified
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    public long updateLastModified(long lastModified) {
        if (this.lastModified < lastModified) {
            this.lastModified = lastModified;
        }
        return this.lastModified;
    }

    /**
     * @return the lastAudit
     */
    @Override
    public long getLastAudit() {
        return lastAudit;
    }

    /**
     * @param lastAudit the lastAudit to set
     */
    public void setLastAudit(long lastAudit) {
        this.lastAudit = lastAudit;
    }

    /**
     * @return the summaryStatus
     */
    @Override
    public TopologyStatus getSummaryStatus() {
        return summaryStatus;
    }

    /**
     * @param summaryStatus the overallStatus to set
     */
    public void setSummaryStatus(TopologyStatus summaryStatus) {
        this.summaryStatus = summaryStatus;
    }
    
    private void summaryAuditStart() {
        // Set the provider status if this is not our first time.
        if (summaryStatus != TopologyStatus.Initializing) {
            summaryStatus = TopologyStatus.Auditing;
        }
        
        setLastAudit(System.currentTimeMillis());
    }

    private void summaryAuditError() {
        summaryStatus = TopologyStatus.Error;
    }
    
    private void summaryAuditSuccess() {
        summaryStatus = TopologyStatus.Completed;
        
        // Determine if any of the component audits have failed.  A manifest
        // failure would already be taggeed as an summary error.
        for (TopologyProviderStatus provider : providerStatus.values()) {
            if (provider.getStatus() == TopologyStatus.Error) {
                summaryStatus = TopologyStatus.PartiallyComplete;
                break;
            }
        }
    }
    
    private void manifestAuditStart() {
        if (manifestStatus == null) {
            manifestStatus = new TopologyProviderStatus();
            
            if (getTopologyManifestReader() != null) {
                manifestStatus.setId(getTopologyManifestReader().getId());
                manifestStatus.setHref(getTopologyManifestReader().getTarget());
            }
        }
        else {
            manifestStatus.setStatus(TopologyStatus.Auditing);
            manifestStatus.setLastAudit(System.currentTimeMillis());
        }
    }
    
    private void manifestAuditError() {
        manifestStatus.setStatus(TopologyStatus.Error);
    }
    
    private void manifestAuditSuccess() {
        manifestStatus.setId(getTopologyManifestReader().getId());
        manifestStatus.setHref(getTopologyManifestReader().getTarget());
        manifestStatus.setStatus(TopologyStatus.Completed);
        manifestStatus.setLastSuccessfulAudit(getManifestStatus().getLastAudit());
        manifestStatus.setLastModified(getTopologyManifestReader().getLastModified());
    }

    /**
     * @return the manifestStatus
     */
    @Override
    public TopologyProviderStatus getManifestStatus() {
        return manifestStatus;
    }
    
    private TopologyProviderStatus providerAuditStart(NmlTopologyReader reader, Map<String, TopologyProviderStatus> newProviderStatus) {
        String id = reader.getId();
        String href = reader.getTarget();
        
        TopologyProviderStatus local = providerStatus.get(id);
        
        if (local != null) {
            // We have an existing entry for this provider so we update it.
            local.setStatus(TopologyStatus.Auditing);
        }
        else {
            // We have a new entry so create a status and set defaults.
            local = new TopologyProviderStatus();
            local.setId(id);
            local.setHref(href);
            local.setStatus(TopologyStatus.Initializing);
        }
        
        local.setLastAudit(System.currentTimeMillis());
        
        // Add into our new status map.
        newProviderStatus.put(id, local);
        return local;
    }
    
    private void providerAuditError(TopologyProviderStatus local) {
        local.setStatus(TopologyStatus.Error);
    }
    
    private void providerAuditSuccess(NmlTopologyReader reader, TopologyProviderStatus local) {
        local.setStatus(TopologyStatus.Completed);
        local.setLastSuccessfulAudit(local.getLastAudit());
        local.setLastModified(reader.getLastModified());
        local.setLastDiscovered(reader.getLastDiscovered());
    }
    
    /**
     * @return the manifestStatus
     */
    @Override
    public Collection<TopologyProviderStatus> getProviderStatus() {
        return providerStatus.values();
    }
    
}
