package net.es.nsi.pce.topology.provider;

import net.es.nsi.pce.topology.model.NsiTopologyFactory;
import java.io.UnsupportedEncodingException;
import net.es.nsi.pce.management.logs.PceLogs;
import net.es.nsi.pce.management.logs.PceErrors;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.es.nsi.pce.topology.model.NsiSdpFactory;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.management.jaxb.TopologyStatusType;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.topology.dao.TopologyConfiguration;


/**
 * Dynamically download the NSI/NML topology from peer NSA, parse, and
 * consolidate into a connected network topology.  The list of NSA offering
 * dynamic download of topology is specified in the XML bootstrap document
 * loaded from the location specified in the TopologyProvider configuration
 * file.
 * 
 * @author hacksaw
 */
public class DdsTopologyProvider implements TopologyProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
      
    // Location of configuration file.
    private TopologyConfiguration configuration;
    
    // List of discovered NSA documents published in the DDS.
    private DdsDocumentReader nsaDocumentReader;
    private Map<String, DdsWrapper> nsaDocuments;
    
    // List of discovered topology documents published in the DDS.
    private DdsDocumentReader topologyDocumentReader;
    private Map<String, DdsWrapper> topologyDocuments;

    // The NSI Topology model used by path finding.
    private NsiTopology nsiTopology = new NsiTopology();
    
    // The last time an NML object was discovered.
    private long lastDiscovered = 0L;
    
    private long lastAudit = 0L;
    
    // Overall discovery status. 
    private TopologyStatusType summaryStatus = TopologyStatusType.INITIALIZING;
    
    // The status of this provider. 
    private TopologyProviderStatus providerStatus = null;
    
    private PceLogger pceLogger = PceLogger.getLogger();
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public DdsTopologyProvider(TopologyConfiguration configuration) {
        this.configuration = configuration;
    }
    
     /**
     * For this provider we read the topology source from an XML configuration
     * file.
     * 
     * @param source Path to the XML configuration file.
     */
    @Override
    public void init() throws Exception {
        nsaDocumentReader = new DdsDocumentReader(configuration.getLocation(), NsiConstants.NSI_DOC_TYPE_NSA_V1);
        topologyDocumentReader = new DdsDocumentReader(configuration.getLocation(), NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
        providerStatus = new TopologyProviderStatus();
        providerStatus.setId(getClass().getName());
        providerStatus.setHref(configuration.getLocation());
        providerStatus.setStatus(TopologyStatusType.INITIALIZING);
    }

    @Override
    public synchronized void loadTopology() throws Exception {
        boolean changed = false;

        // Identify that we have started an audit.
        ddsAuditStart();
        
        // Get an updated copy of the NSA discovery documents.
        Map<String, DdsWrapper> newNsadocs;
        try {
            newNsadocs = nsaDocumentReader.getIfModified();
        }
        catch (NotFoundException | JAXBException | UnsupportedEncodingException ex) {
            // Identify that we have failed the audit.
            ddsAuditError();
            log.error("loadNetworkTopology: Failed to load NSA discovery documents from DDS.", ex);
            throw ex;
        }
        
        // See if the NSA document list has changed.
        if (newNsadocs != null) {
            nsaDocuments = newNsadocs;
            changed = true;
        }

        // Check to see if we have NSA discovery documents.
        if (nsaDocuments == null) {
            ddsAuditError();
            log.error("loadNetworkTopology: No NSA discovery documents found in DDS.");
            return;            
        }
        
        // Get an updated copy of the Topology documents.
        Map<String, DdsWrapper> newTopologyDocs;
        try {
            newTopologyDocs = topologyDocumentReader.getIfModified();
        }
        catch (NotFoundException | JAXBException | UnsupportedEncodingException ex) {
            // Identify that we have failed the audit.
            ddsAuditError();
            log.error("loadNetworkTopology: Failed to load topology documents.", ex);
            throw ex;
        }
        
        // See if the topology document list has changed.
        if (newTopologyDocs != null) {
            topologyDocuments = newTopologyDocs;
            changed = true;
        }

        // Check to see if we have topology documents.
        if (topologyDocuments == null) {
            // Identify that we have failed the audit.
            ddsAuditError();
            log.error("loadNetworkTopology: Failed to load Topology documents.");
            throw new NotFoundException("Failed to load topology documents.");               
        }
        
        // We had a change in the discovered data so process.
        if (!changed) {
            log.debug("loadNetworkTopology: no change in topology.");
            ddsAuditSuccess();
            return;
        }

        NsiTopology newTopology;
        NsiTopologyFactory nsiFactory = new NsiTopologyFactory();
        nsiFactory.setDefaultServiceType(configuration.getDefaultServiceType());
        newTopology = nsiFactory.createNsiTopology(nsaDocuments, topologyDocuments);
        lastDiscovered = nsiFactory.getLastDiscovered();
        newTopology = consolidateGlobalTopology(newTopology);
        newTopology.setLastDiscovered(lastDiscovered);

        // Identify a successful audit.
        ddsAuditSuccess();
        
        // We are done so update the map with the new view.   
        nsiTopology = newTopology;
    }
    
    private NsiTopology consolidateGlobalTopology(NsiTopology topology) {
        topology.addAllSdp(NsiSdpFactory.createUnidirectionalSdpTopology(topology.getStpMap()));
        topology.addAllSdp(NsiSdpFactory.createBidirectionalSdps(topology.getStpMap()));
        return topology;
    }
    
    @Override
    public NsiTopology getTopology() {
        return nsiTopology;
    }
    
    /**
     * For this provider we read the topology source from an XML configuration
     * file.
     * 
     * @param source Path to the XML configuration file.
     */
    @Override
    public void setConfiguration(TopologyConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the source file of the topology configuration.
     * 
     * @return The path to the XML configuration file. 
     */
    @Override
    public TopologyConfiguration getConfiguration() {
        return configuration;
    }    

    /**
     * @return the lastModified
     */
    @Override
    public long getLastDiscovered() {
        return lastDiscovered;
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
    public TopologyStatusType getAuditStatus() {
        return summaryStatus;
    }
    
    private void ddsAuditStart() {
        long auditTime = System.currentTimeMillis();
        
        // Let the logger know we are starting another topology discovery run.
        pceLogger.setAuditTimeStamp(auditTime);
        pceLogger.logAudit(PceLogs.AUDIT_START, "DsdTopologyProvider", "Full topology audit starting.");
        
        // Set the provider status if this is not our first time.
        if (summaryStatus != TopologyStatusType.INITIALIZING) {
            summaryStatus = TopologyStatusType.AUDITING;
            providerStatus.setStatus(TopologyStatusType.AUDITING);
        }

        providerStatus.setLastAudit(auditTime);
    }

    private void ddsAuditError() {
        pceLogger.errorAudit(PceErrors.AUDIT, "DsdTopologyProvider");
        summaryStatus = TopologyStatusType.ERROR;
        providerStatus.setStatus(TopologyStatusType.ERROR);
        pceLogger.clearAuditTimeStamp();
    }
    
    private void ddsAuditSuccess() {
        summaryStatus = TopologyStatusType.COMPLETED;
        
        providerStatus.setStatus(TopologyStatusType.COMPLETED);
        providerStatus.setLastSuccessfulAudit(providerStatus.getLastAudit());
        providerStatus.setLastDiscovered(lastDiscovered);
        
        pceLogger.logAudit(PceLogs.AUDIT_SUCCESSFUL, "DsdTopologyProvider", "Topology audit completed successfully.");        
        pceLogger.clearAuditTimeStamp();
    }

    /**
     * @return the manifestStatus
     */
    @Override
    public TopologyProviderStatus getProviderStatus() {
        return providerStatus;
    }

    /**
     * @return the auditInterval
     */
    @Override
    public long getAuditInterval() {
        return configuration.getAuditInterval();
    }
}
