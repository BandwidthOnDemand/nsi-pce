package net.es.nsi.pce.topology.provider;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.dds.DocumentListType;
import net.es.nsi.pce.jaxb.management.TopologyStatusType;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.management.logs.PceLogs;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.spring.SpringApplicationContext;
import net.es.nsi.pce.topology.dao.TopologyConfiguration;
import net.es.nsi.pce.topology.model.ControlPlaneTopology;
import net.es.nsi.pce.topology.model.NsiSdpFactory;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.model.NsiTopologyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private DocumentReader nsaDocumentReader;
    private Map<String, DdsWrapper> nsaDocuments;
    private DocumentListType localNsaDocuments;

    // List of discovered topology documents published in the DDS.
    private DocumentReader topologyDocumentReader;
    private Map<String, DdsWrapper> topologyDocuments = new ConcurrentHashMap<>();
    private DocumentListType localTopologyDocuments;

    // The NSI Topology model used by path finding.
    private NsiTopology nsiTopology = new NsiTopology();

    private ControlPlaneTopology controlPlaneTopology;

    // The last time an NML object was discovered.
    private long lastDiscovered = 0L;

    // The status of this provider.
    private TopologyProviderStatus providerStatus = null;
    private long auditTime = 0;

    private final PceLogger pceLogger = PceLogger.getLogger();

    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     *
     * @param configuration
     */
    public DdsTopologyProvider(TopologyConfiguration configuration) {
        this.configuration = configuration;
        providerStatus = new TopologyProviderStatus();
        providerStatus.setId(getClass().getName());
        providerStatus.setHref(configuration.getDdsURL());
        providerStatus.setStatus(TopologyStatusType.INITIALIZING);
    }

    public static DdsTopologyProvider getInstance() {
        DdsTopologyProvider tp = SpringApplicationContext.getBean("topologyProvider", DdsTopologyProvider.class);
        return tp;
    }

     /**
     * For this provider we read the topology source from an XML configuration
     * file.
     *
     * @throws java.lang.Exception
     */
    @Override
    public void init() throws Exception {
        nsaDocumentReader = SpringApplicationContext.getBean("documentReader", DocumentReader.class);
        nsaDocumentReader.setTarget(configuration.getDdsURL());
        nsaDocumentReader.setType(NsiConstants.NSI_DOC_TYPE_NSA_V1);

        topologyDocumentReader = SpringApplicationContext.getBean("documentReader", DocumentReader.class);
        topologyDocumentReader.setTarget(configuration.getDdsURL());
        topologyDocumentReader.setType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
    }

    @Override
    public synchronized boolean loadTopology() throws Exception {
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
            return false;
        }

        // See if the NSA document list has changed.
        if (newNsadocs != null) {
            nsaDocuments = newNsadocs;
            localNsaDocuments = nsaDocumentReader.getLocalDocuments();
            changed = true;
        }

        // Check to see if we have NSA discovery documents.
        if (localNsaDocuments.getDocument().isEmpty()) {
            ddsAuditError();
            log.error("loadNetworkTopology: No local NSA discovery documents found in DDS.");
            return false;
        }

        // Check to see if we have NSA discovery documents.
        if (nsaDocuments == null) {
            ddsAuditError();
            log.error("loadNetworkTopology: No NSA discovery documents found in DDS.");
            return false;
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
            return false;
        }

        // See if the topology document list has changed.
        if (newTopologyDocs != null) {
            topologyDocuments = newTopologyDocs;
            localTopologyDocuments = topologyDocumentReader.getLocalDocuments();
            changed = true;
        }

        // Check to see if we have topology documents.
        if (topologyDocuments == null) {
            // Identify that we have failed the audit.
            ddsAuditError();
            log.error("loadNetworkTopology: Failed to load Topology documents.");
            return false;
        }

        // If no change then get out of here.
        if (!changed) {
            log.debug("loadNetworkTopology: no change in topology.");
            ddsAuditSuccess();
            return true;
        }

        // Now that we have a new set of documents (at least some were new or
        // modified), we must rebuild our entire topology model.
        NsiTopologyFactory nsiFactory = new NsiTopologyFactory();
        nsiFactory.setDefaultServiceType(configuration.getDefaultServiceType());
        nsiFactory.setBaseURL(configuration.getBaseURL());
        NsiTopology newTopology = nsiFactory.createNsiTopology(localNsaDocuments, nsaDocuments, localTopologyDocuments, topologyDocuments);
        lastDiscovered = nsiFactory.getLastDiscovered();
        newTopology = consolidateGlobalTopology(newTopology);
        newTopology.setLastDiscovered(lastDiscovered);
        controlPlaneTopology = new ControlPlaneTopology(newTopology);

        // Identify a successful audit.
        ddsAuditSuccess();

        // We are done so update the map with the new view.
        nsiTopology = newTopology;
        return true;
    }

    private NsiTopology consolidateGlobalTopology(NsiTopology topology) {
        log.debug("consolidateGlobalTopology: **** START CONSOLIDATING SDPs ****");
        log.debug("consolidateGlobalTopology: processing " + topology.getStpMap().size() + " STPs");
        topology.addAllSdp(NsiSdpFactory.createUnidirectionalSdp(topology.getStpMap()));
        topology.addAllSdp(NsiSdpFactory.createBidirectionalSdps(topology.getStpMap()));
        log.debug("consolidateGlobalTopology: **** COMPLETED CONSOLIDATING SDPs ****");
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

    private void ddsAuditStart() {
        auditTime = System.currentTimeMillis();

        // Let the logger know we are starting another topology discovery run.
        pceLogger.setAuditTimeStamp(auditTime);
        pceLogger.logAudit(PceLogs.AUDIT_START, "DdsTopologyProvider", "Full topology audit starting.");

        // Set the provider status if this is not our first time.
        if (providerStatus.getStatus() != TopologyStatusType.INITIALIZING) {
            providerStatus.setStatus(TopologyStatusType.AUDITING);
        }

        providerStatus.setLastAudit(auditTime);
    }

    private void ddsAuditError() {
        pceLogger.errorAudit(PceErrors.AUDIT, "DdsTopologyProvider");
        providerStatus.setStatus(TopologyStatusType.ERROR);
        providerStatus.setLastAuditDuration(System.currentTimeMillis() - auditTime);
        pceLogger.clearAuditTimeStamp();
    }

    private void ddsAuditSuccess() {
        providerStatus.setStatus(TopologyStatusType.COMPLETED);
        providerStatus.setLastSuccessfulAudit(providerStatus.getLastAudit());
        providerStatus.setLastDiscovered(lastDiscovered);
        providerStatus.setLastAuditDuration(System.currentTimeMillis() - auditTime);

        pceLogger.logAudit(PceLogs.AUDIT_SUCCESSFUL, "DdsTopologyProvider", "Topology audit completed successfully.");
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

    /**
     * @return the ControlPlaneTopology
     */
    @Override
    public ControlPlaneTopology getControlPlaneTopology() {
        return controlPlaneTopology;
    }
}
