package net.es.nsi.pce.config.topo;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.config.topo.nml.BidirectionalEthernetPort;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.TopologyManifest;
//import net.es.nsi.pce.pf.api.topo.Network;
//import net.es.nsi.pce.pf.api.topo.Nsa;
import net.es.nsi.pce.pf.api.topo.NsiTopology;
//import net.es.nsi.pce.pf.api.topo.Sdp;
//import net.es.nsi.pce.pf.api.topo.Stp;
//import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.config.jaxb.ConfigurationType;
import net.es.nsi.pce.schema.TopologyConfigurationParser;
//import net.es.nsi.pce.nml.jaxb.NSAType;
//import net.es.nsi.pce.nml.jaxb.TopologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;


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

    // Our topology manifest.
    private TopologyManifest topologyManifest;
    
    // NSA topology entries indexed by string URL.
    private Map<String, NmlTopologyReader> topologyUrlMap = new ConcurrentHashMap<>();

    // Map holding the network topologies indexed by network Id.
    private Map<String, net.es.nsi.pce.nml.jaxb.NSAType> nsas = new ConcurrentHashMap<>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, net.es.nsi.pce.nml.jaxb.TopologyType> topologies = new ConcurrentHashMap<>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<>();

    private NsiTopology nsiTopology = new NsiTopology();
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Date date;

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
    
    private void loadTopologyConfiguration() throws JAXBException, FileNotFoundException {
        ConfigurationType config = TopologyConfigurationParser.getInstance().parse(configuration);
        
        setLocation(config.getLocation());
        
        if (config.getAuditInterval() > 0) {
            setAuditInterval(config.getAuditInterval());
        }

        topologyManifestReader.setTarget(getLocation());
    }

    private void loadNetworkTopology() throws Exception {
        if (log.isDebugEnabled()) {
            date = new Date();
            log.debug("----------------------------------------------------------");
            log.debug("loadNetworkTopology(): Starting " + dateFormat.format(date));
        }

        loadTopologyConfiguration();
                
        // Get an updated copy of the topology manifest file.
        TopologyManifest newManifest;
        try {
            newManifest = getTopologyManifestReader().getManifestIfModified();
        }
        catch (Exception ex) {
            log.error("loadNetworkTopology: Failed to load topology manifest " + getTopologyManifestReader().getTarget(), ex);
            throw ex;
        }
        
        // See if the topology manifest list has changed.
        if (newManifest != null) {
            topologyManifest = newManifest;
        }
        
        // Check to see if we have a valid topology manifest.
        if (topologyManifest == null) {
            log.error("loadNetworkTopology: Failed to load topology manifest " + getTopologyManifestReader().getTarget());
            throw new NotFoundException("Failed to load topology manifest " + getTopologyManifestReader().getTarget());               
        }
        
        // Create a new topology URL map.
        Map<String, NmlTopologyReader> newTopologyUrlMap = new ConcurrentHashMap<>();
        
        // Load each topology from supplied endpoint.  If we fail to load a new
        // topology, then keep the old one for now.
        for (String entry : topologyManifest.getEntryList().values()) {
            // Check to see if we have already discovered this NSA.
            NmlTopologyReader originalReader = topologyUrlMap.get(entry);
            NmlTopologyReader reader;
            if (originalReader == null) {
                // We not have one so create a new reader.
                log.debug("loadNetworkTopology: creating new reader for " + entry);
                reader = topologyReaderFactory.getReader(entry);
            }
            else {
                reader = originalReader;
            }
            
            // Attempt to discover this topology.
            try {
                reader.load();
            }
            catch (Exception ex) {
                // If we have already read this topology successfully, then
                // keep it on failure.
                log.error("loadTopology: Failed to load NSA topology: " + entry, ex);
                if (originalReader == null) {
                    // We have never successfully discovered this NSA so skip.
                    continue;
                }
            }
            
            newTopologyUrlMap.put(entry, reader);
        }

        // We are done so update the map with the new view.
        topologyUrlMap = newTopologyUrlMap;
        
        // Consolidate individual topologies in one master list.
        Map<String, net.es.nsi.pce.nml.jaxb.NSAType> newNsas = new ConcurrentHashMap<>();
        Map<String, net.es.nsi.pce.nml.jaxb.TopologyType> newTopologies = new ConcurrentHashMap<>();
        Map<String, EthernetPort> newEthernetPorts = new ConcurrentHashMap<>();

        for (NmlTopologyReader nml : topologyUrlMap.values()) {
            net.es.nsi.pce.nml.jaxb.NSAType nsa = nml.getNsa();
            if (nsa.getId() == null || nsa.getId().isEmpty()) {
                log.error("Topology endpoint missing NSA identifier: " + nml.getTarget());
                continue;
            }
            
            log.debug("Processing topology for " + nml.getNsa().getId());
            
            newNsas.put(nsa.getId(), nsa);
            newTopologies.putAll(nml.getTopologies());
            newEthernetPorts.putAll(nml.getEthernetPorts());
        }
        
        // Interconnect bidirectional ports using their unidirectional components.
        for (EthernetPort ethPort : newEthernetPorts.values()) {
            log.debug("Processing port " + ethPort.getPortId());
            
            if (ethPort.isBidirectional() && ethPort instanceof BidirectionalEthernetPort) {
                log.debug("Consolidating bidirectional link: " + ethPort.getPortId());
                BidirectionalEthernetPort biPort = (BidirectionalEthernetPort) ethPort;
                
                // Unidirectional links are in Bidirectional port definition.
                EthernetPort inbound = biPort.getInbound();
                EthernetPort outbound = biPort.getOutbound();
                
                // Verify the inbound peer unidirectional ports exist and is connected.
                EthernetPort remoteOutbound = null;
                if (inbound.getConnectedTo().isEmpty()) {
                    log.debug("Bidirectional port " + biPort.getPortId() + " has no connectedTo information for inbound port: " + inbound.getPortId());
                }
                else {
                    // Assume only one for now.
                    String remotePortId = inbound.getConnectedTo().get(0);
                    remoteOutbound = newEthernetPorts.get(remotePortId);
                
                    if (remoteOutbound == null) {
                        log.error("Bidirectional port " + biPort.getPortId() + " has inbound unidirectional port " + inbound.getPortId() + " with bad remote reference " + remotePortId);
                    }
                }
                
                // Verify the outbound peer unidirectional ports exist.
                EthernetPort remoteInbound = null;
                if (outbound.getConnectedTo().isEmpty()) {
                    log.debug("Bidirectional port " + biPort.getPortId() + " has no connectedTo information for inbound port: " + outbound.getPortId());
                }
                else {
                    // Assume only one for now.
                    String remotePortId = outbound.getConnectedTo().get(0);
                    remoteInbound = newEthernetPorts.get(remotePortId);

                    if (remoteInbound == null) {
                        log.debug("Bidirectional port " + biPort.getPortId() + " has outbound unidirectional port " + outbound.getPortId() + " with bad remote reference " + remotePortId);
                    }
                }
                
                // We need valid connectivity information before we can consolidate links.
                if (remoteOutbound == null && remoteInbound == null) {
                    // This must be a client "Uni" port with not connectivity information.
                    log.error("Bidirectional port " + biPort.getPortId() + " has no remote port references (Uni?).");
                }
                else if (remoteOutbound == null || remoteInbound == null) {
                    log.error("Bidirectional port " + biPort.getPortId() + " cannot be consolidated due one missing remote unidirectional port reference.");
                }
                else {
                    // Verify the remote ports also think they are connected to these ports.
                    if (!inbound.getConnectedTo().contains(remoteOutbound.getPortId()) ||
                            !remoteOutbound.getConnectedTo().contains(inbound.getPortId())) {
                        log.error("Port " + inbound.getPortId() + " indicates connectivity to " + remoteOutbound.getPortId() + " but this port does not agree!");
                    }
                    else if (!outbound.getConnectedTo().contains(remoteInbound.getPortId()) ||
                            !remoteInbound.getConnectedTo().contains(outbound.getPortId())) {
                        log.error("Port " + outbound.getPortId() + " indicates connectivity to " + remoteInbound.getPortId() + " but this port does not agree!");
                    }
                    else {
                        // We have remote port references, now find the remote bidirectional port.
                        BidirectionalEthernetPort remotePort = findBidirectionalEthernetPortByMemberId(newEthernetPorts, remoteInbound.getPortId(), remoteOutbound.getPortId());
                        if (remotePort == null) {
                            log.error("BidirectionalPort " + biPort.getPortId() + " has no compatible topology peer for inbound " + remoteInbound.getPortId() + ", and outbound " + remoteOutbound.getPortId());
                        }
                        else {
                            log.debug("Consolidating bidirectional ports: " + biPort.getPortId() + " and " + remotePort.getPortId());
                            biPort.getConnectedTo().add(remotePort.getPortId());
                            biPort.setRemotePort(remotePort);
                        }
                    }
                }
            }
        }
        
        // Update the gloabl topology with the new view.
        nsas = newNsas;
        topologies = newTopologies;
        ethernetPorts = newEthernetPorts;
        
        if (log.isDebugEnabled()) {
            date = new Date();
            log.debug("PollingTopologyProvider.loadNetworkTopology(): Ending " + dateFormat.format(date));
            log.debug("----------------------------------------------------------");
        }
    }
   
    private BidirectionalEthernetPort findBidirectionalEthernetPortByMemberId(Map<String, EthernetPort> ethernetPorts, String inbound, String outbound) {
        for (EthernetPort ethPort : ethernetPorts.values()) {
            if (ethPort.isBidirectional() && ethPort instanceof BidirectionalEthernetPort) {
                BidirectionalEthernetPort biPort = (BidirectionalEthernetPort) ethPort;
                
                if ((biPort.getInbound() != null && biPort.getInbound().getPortId().equalsIgnoreCase(inbound)) &&
                        (biPort.getOutbound() != null && biPort.getOutbound().getPortId().equalsIgnoreCase(outbound))) {
                    return biPort;
                }
            }
        }
        
        return null;
    }

    /**
     * Take the NML topology for the set of interconnected networks and compute
     * the NSI related topology objects (Network, STP, and SDP) used for path
     * calculations.
     * 
     * @throws Exception If there are invalid topologies.
     */
    @Override
    public synchronized void loadTopology() throws Exception {
        if (log.isDebugEnabled()) {
            date = new Date();
            log.debug("----------------------------------------------------------");
            log.debug("PollingTopologyProvider.loadTopology(): Starting " + dateFormat.format(date));
        }
        
        // Load the NML topology model.
        loadNetworkTopology();

        // Create the NSI topology.
        NsiTopology newNsiTopology = new NsiTopology();

        /* Each NML object must be mapped through to an equivalent NSI Topology
         * object.  Ww start by parsing the NSA objects, extracting Topology
         * objects to created networks.
         */
        for (net.es.nsi.pce.nml.jaxb.NSAType nmlNsa : nsas.values()) {
            // Create the NSI NSA object.
            NsaType nsiNsa = new NsaType();
            nsiNsa.setId(nmlNsa.getId());
            nsiNsa.setHref("unset");
            nsiNsa.setName(nmlNsa.getName());
            if (nmlNsa.getLocation() != null) {
                nsiNsa.setLatitude(nmlNsa.getLocation().getLat());
                nsiNsa.setLongitude(nmlNsa.getLocation().getLong());
            }
            
            // Create the NSA resource reference.
            ResourceRefType nsiNsaRef  = new ResourceRefType();
            nsiNsaRef.setId(nmlNsa.getId());
            nsiNsaRef.setHref("unset");            
            
            // Process each Topology object under this NSA object.
            for (net.es.nsi.pce.nml.jaxb.TopologyType nmlTopology : nmlNsa.getTopology()) {
                // Create a new NSI Network object.
                NetworkType nsiNetwork = new NetworkType();
                nsiNetwork.setId(nmlTopology.getId());
                String name = nmlTopology.getName();
                if (name == null || name.isEmpty()) {
                    name = nmlTopology.getId();
                }
                nsiNetwork.setName(name);

                // Add this network object to our NSI topology.
                newNsiTopology.addNetwork(nsiNetwork);
                
                // We need to build a reference to this new Network object for
                // use in the NSA object.
                ResourceRefType nsiNetworkRef  = new ResourceRefType();
                nsiNetworkRef.setId(nmlTopology.getId());
                nsiNetworkRef.setHref("unset");
                
                nsiNsa.getNetwork().add(nsiNetworkRef);
            }
            
            // Add this compeleted NSA object to the NSI topology.
            newNsiTopology.addNsa(nsiNsa);
        }

        // Add each bidirectional port as a bidirectional STP in the NSI topology.
        Map<String, EthernetPort> ethPorts = new ConcurrentHashMap<>(ethernetPorts);
        for (String key : ethPorts.keySet()) {
            EthernetPort localPort = ethPorts.remove(key);
            
            // Make sure we have not already processed the port as a remote end
            // of a previous iteration.
            if (localPort != null && localPort.isBidirectional()) {
                log.debug("Converting Ethernet port to STP: " + localPort.getPortId());
                
                BidirectionalEthernetPort localBiPort = (BidirectionalEthernetPort) localPort;
                NetworkType nsiLocalNetwork = newNsiTopology.getNetworkById(localBiPort.getTopologyId());
                
                // This should never happen but skip the port if it does.
                if (nsiLocalNetwork == null) {
                    log.error("Could not find network " + localBiPort.getTopologyId());
                    continue;
                }

                // We create an STP per vlan for the local Ethernet port.  They
                // are created even if the remote end does not have a matching
                // STP.  Why?  Logically these unconnected STP exist even though
                // they can never be used.
                HashMap<Integer, StpType> processedStp = new HashMap<>();
                
                for (Integer vlanId : localBiPort.getVlans()) {
                    log.debug("Converting local port " + localBiPort.getPortId() + " vlan = " + vlanId);
                    
                    // Create the local STP.
                    StpType nsiLocalStp = newNsiTopology.newStp(nsiLocalNetwork, localBiPort, vlanId);
                    
                    // Create a reference to this new STP.
                    ResourceRefType nsiLocalStpRef = newNsiTopology.newStpRef(nsiLocalStp);
                    
                    // Update this network to hold the STP reference.
                    nsiLocalNetwork.getStp().add(nsiLocalStpRef);
                    
                    // Add this STP to the NSI Topology.
                    newNsiTopology.addStp(nsiLocalStp);
                    
                    // Save it for SDP processing.
                    processedStp.put(vlanId, nsiLocalStp);
                }
                
                // We only process remote end STP if one exists.  If there is
                // no remote end then we do not create an SDP.
                BidirectionalEthernetPort remoteBiPort = localBiPort.getRemotePort();
                NetworkType nsiRemoteNetwork;
                if (remoteBiPort != null) {
                    log.debug("Found remote biport for conversion " + remoteBiPort.getPortId());
                    
                    // Remove remote port matching the local port so we do not
                    // process it twice.
                    ethPorts.remove(remoteBiPort.getPortId());
                    nsiRemoteNetwork = newNsiTopology.getNetworkById(remoteBiPort.getTopologyId());
                    
                    // Create the remote STP so we don't need to do it later
                    // when visiting the remote network.
                    for (Integer vlanId : remoteBiPort.getVlans()) {
                        log.debug("Converting remote port " + remoteBiPort.getPortId() + " vlan = " + vlanId);

                        // Create the remote STP and store in remote network topology.
                        StpType nsiRemoteStp = newNsiTopology.newStp(nsiRemoteNetwork, remoteBiPort, vlanId);
                        
                        // Create a reference to this new remote STP.
                        ResourceRefType nsiRemoteStpRef = newNsiTopology.newStpRef(nsiRemoteStp);
                        
                        // Update this network to hold the STP reference.
                        nsiRemoteNetwork.getStp().add(nsiRemoteStpRef);
                    
                        // Add this STP to the NSI Topology.
                        newNsiTopology.addStp(nsiRemoteStp);
                        
                        // Create an SDP if there is a matching local STP.
                        StpType nsiLocalStp = processedStp.get(vlanId);
                        if (nsiLocalStp != null) {
                            SdpType nsiSdp = newNsiTopology.newSdp(nsiLocalStp, nsiRemoteStp);
                            newNsiTopology.addSdp(nsiSdp);
                            log.debug("Added SDP: " + nsiSdp.getId());                            
                        }
                    }          
                }
                else {
                    // This is okay if the port is considered a client UNI port.
                    log.info("No topology link for port " + localPort.getPortId());
                }
            }
        }
 
        nsiTopology = newNsiTopology;
        
        if (log.isDebugEnabled()) {
            // Dump Netoworks for debug.
            log.debug("The following Networks were created:");
            for (NetworkType network : nsiTopology.getNetworks()) {
                log.debug("    " + network.getId());
            }

            // Dump SDP links for debug.
            log.debug("The following SDP links were created:");
            for (SdpType sdp : nsiTopology.getSdps()) {
                log.debug("    " + sdp.getId());
            }

            date = new Date();
            log.debug("PollingTopologyProvider.loadTopology(): Ending " + dateFormat.format(date));
            log.debug("----------------------------------------------------------");
        }
    }
    
    @Override
    public NsiTopology getTopology() {
        return nsiTopology;
    }
    
    @Override
    public Set<String> getNetworkIds() {
        return nsiTopology.getNetworkIds();
    }

    @Override
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
}
