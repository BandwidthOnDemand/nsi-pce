package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.schema.XmlParser;
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
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.MasterTopology;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Nsa;
import net.es.nsi.pce.pf.api.topo.Sdp;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.topology.jaxb.ConfigurationType;
import net.es.nsi.pce.topology.jaxb.NSAType;
import net.es.nsi.pce.topology.jaxb.TopologyType;
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
public class PollingTopologyProvider implements TopologyProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Location of configuration file.
    private String configFile = "config/topology.xml";
    
    // Remote master topology enpoint.
    private String remoteEndpoint = null;
    
    // Time between topology refreshes.
    private long auditInterval = 30*60*1000;  // 30 minute polling time.
    
    // Our master topology provider.
    private MasterTopologyProvider masterTopologyProvider;
    
    // Our master topology provider.
    private MasterTopology masterTopology;
    
    // NSA topology entries indexed by string URL.
    private Map<String, NsaTopologyProvider> topologyUrlMap = new ConcurrentHashMap<String, NsaTopologyProvider>();

    // Map holding the network topologies indexed by network Id.
    private Map<String, NSAType> nsas = new ConcurrentHashMap<String, NSAType>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, TopologyType> topologies = new ConcurrentHashMap<String, TopologyType>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<String, EthernetPort>();

    private Topology nsiTopology = new Topology();
    
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
        this.configFile = configFile;
    }

    /**
     * @return the configFile
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * @param configFile the configFile to set
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * @return the remoteEndpoint
     */
    public String getRemoteEndpoint() {
        return remoteEndpoint;
    }

    /**
     * @param remoteEndpoint the remoteEndpoint to set
     */
    public void setRemoteEndpoint(String remoteEndpoint) {
        this.remoteEndpoint = remoteEndpoint;
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
        ConfigurationType config = XmlParser.getInstance().parseTopologyConfiguration(configFile);
        
        setRemoteEndpoint(config.getRemoteEndpoint());
        
        if (config.getAuditInterval() > 0) {
            setAuditInterval(config.getAuditInterval());
        }
        
        // Create our master topology provider but don't load data just yet.
        masterTopologyProvider = new MasterTopologyProvider(getRemoteEndpoint());
    }

    private void loadNetworkTopology() throws Exception {
        if (log.isDebugEnabled()) {
            date = new Date();
            log.debug("----------------------------------------------------------");
            log.debug("loadNetworkTopology(): Starting " + dateFormat.format(date));
        }

        loadTopologyConfiguration();
                
        // Get an updated copy of the master topology file.
        MasterTopology newMaster;
        try {
            newMaster = masterTopologyProvider.getMasterTopologyIfModified();
        }
        catch (Exception ex) {
            log.error("loadNetworkTopology: Failed to load master topology.", ex);
            throw ex;
        }
        
        // See if the master topology list has changed.
        if (newMaster != null) {
            masterTopology = newMaster;
        }
        
        // Check to see if we have a valid master topology.
        if (masterTopology == null) {
            log.error("loadNetworkTopology: Failed to load master topology " + masterTopologyProvider.getTarget());
            throw new NotFoundException("Failed to load master topology " + masterTopologyProvider.getTarget());               
        }
        
        // Create a new topology URL map.
        Map<String, NsaTopologyProvider> newTopologyUrlMap = new ConcurrentHashMap<String, NsaTopologyProvider>();
        
        // Load each topology from supplied endpoint.
        for (String endpoint : masterTopology.getEntryList().values()) {
            NsaTopologyProvider provider = topologyUrlMap.get(endpoint);
            if (provider == null) {
                provider = new NsaTopologyProvider(endpoint);
            }
            
            try {
                provider.loadTopology();
            }
            catch (Exception ex) {
                log.error("loadTopology: Failed to load NSA topology: " + endpoint, ex);
                continue;
            }
            
            newTopologyUrlMap.put(endpoint, provider);
        }

        // We are done so update the map with the new view.
        topologyUrlMap = newTopologyUrlMap;
        
        // Consolidate individual topologies in one master list.
        Map<String, NSAType> newNsas = new ConcurrentHashMap<String, NSAType>();
        Map<String, TopologyType> newTopologies = new ConcurrentHashMap<String, TopologyType>();
        Map<String, EthernetPort> newEthernetPorts = new ConcurrentHashMap<String, EthernetPort>();

        for (NsaTopologyProvider nml : topologyUrlMap.values()) {
            NSAType nsa = nml.getNsa();
            if (nsa.getId() == null || nsa.getId().isEmpty()) {
                log.error("Topology endpoint missing NSA identifier: " + nml.getTopologySource());
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
            log.debug("DynamicTopologyProvider.loadTopology(): Starting " + dateFormat.format(date));
        }
        
        // Load the NML topology model.
        loadNetworkTopology();

        // Create the NSI STP network topology.
        Topology newNsiTopology = new Topology();
        
        for (NSAType nsaType : nsas.values()) {
            Nsa nsa = new Nsa();
            nsa.setId(nsaType.getId());
            nsa.setName(nsaType.toString());
            if (nsaType.getLocation() != null) {
                nsa.setLatitude(nsaType.getLocation().getLat());
                nsa.setLongitude(nsaType.getLocation().getLong());
            }
            
            for (TopologyType topology : nsaType.getTopology()) {
                nsa.addNetworkId(topology.getId());
                
                Network net = new Network();
                net.setNetworkId(topology.getId());
                String name = topology.getName();
                if (name == null || name.isEmpty()) {
                    name = topology.getId();
                }

                net.setName(name);
                net.setNsaId(nsaType.getId());
                newNsiTopology.addNetwork(net);                
            }
            
            newNsiTopology.addNsa(nsa);
        }

        // Add each bidirectional port as a bidirectional STP in the NSI topology.
        Map<String, EthernetPort> ethPorts = new ConcurrentHashMap<String, EthernetPort>(ethernetPorts);
        for (String key : ethPorts.keySet()) {
            EthernetPort localPort = ethPorts.remove(key);
            
            // Make sure we have not already processed the port as a remote end
            // of a previous iteration.
            if (localPort != null && localPort.isBidirectional()) {
                log.debug("Converting Ethernet port to STP: " + localPort.getPortId());
                
                BidirectionalEthernetPort localBiPort = (BidirectionalEthernetPort) localPort;
                Network localNetwork = newNsiTopology.getNetworkById(localBiPort.getTopologyId());
                
                // This should never happen but skip the port if it does.
                if (localNetwork == null) {
                    log.error("Could not find network " + localBiPort.getTopologyId());
                    continue;
                }

                // We create an STP per vlan for the local Ethernet port.  There
                // are created even if the remote end does not have a matching
                // STP.  Why?  Logically these unconnected STP exist even though
                // they can never be used.
                HashMap<Integer, Stp> processedStp = new HashMap<Integer, Stp>();
                
                for (Integer vlanId : localBiPort.getVlans()) {
                    log.debug("Converting local port " + localBiPort.getPortId() + " vlan = " + vlanId);
                    
                    // Create the local STP and store in network topology.
                    Stp localStp = localNetwork.newStp(localBiPort, vlanId);
                    localNetwork.put(localStp.getId(), localStp);
                    
                    // Save it for SDP processing.
                    processedStp.put(vlanId, localStp);
                }
                
                // We only process remote end STP if one exists.  If there is
                // no remote end then we do not create an SDP either.
                BidirectionalEthernetPort remoteBiPort = localBiPort.getRemotePort();
                Network remoteNetwork;
                if (remoteBiPort != null) {
                    log.debug("Found remote biport for conversion " + remoteBiPort.getPortId());
                    
                    // Remove remote port matching the local port so we do not
                    // process it twice.
                    ethPorts.remove(remoteBiPort.getPortId());
                    remoteNetwork = newNsiTopology.getNetworkById(remoteBiPort.getTopologyId());
                    
                    // Create the remote STP so we don't need to do it later
                    // when visiting the remote network.
                    for (Integer vlanId : remoteBiPort.getVlans()) {
                        log.debug("Converting remote port " + remoteBiPort.getPortId() + " vlan = " + vlanId);

                        // Create the remote STP and store in remote network topology.
                        Stp remoteStp = remoteNetwork.newStp(remoteBiPort, vlanId);
                        remoteNetwork.put(remoteStp.getId(), remoteStp);
                        
                        // Create an SDP if there is a matching local STP.
                        Stp localStp = processedStp.get(vlanId);
                        if (localStp != null) {
                            Sdp sdp = new Sdp();
                            sdp.setA(localStp);
                            sdp.setZ(remoteStp);
                            sdp.setDirectionality(Directionality.bidirectional);
                            newNsiTopology.addSdp(sdp);
                            log.debug("Added SDP: " + sdp.getId());                            
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
            for (Network network : nsiTopology.getNetworks()) {
                log.debug("    " + network.getNetworkId());
            }

            // Dump SDP links for debug.
            log.debug("The following SDP links were created:");
            for (Sdp sdp : nsiTopology.getSdps()) {
                log.debug("    " + sdp.getId());
            }

            date = new Date();
            log.debug("PollingTopologyProvider.loadTopology(): Ending " + dateFormat.format(date));
            log.debug("----------------------------------------------------------");
        }
    }
    
    @Override
    public Topology getTopology() {
        return nsiTopology;
    }
    
    @Override
    public Set<String> getNetworkIds() {
        return nsiTopology.getNetworkIds();
    }

    public Collection<Network> getNetworks() {
        return nsiTopology.getNetworks();
    }
    
    @Override
    public void setTopologySource(String source) {
        //this.setTarget(source);
    }

    @Override
    public String getTopologySource() {
        //return this.getTarget();
        return null;
    }

    /**
     * @return the topologies
     */
    public Map<String, TopologyType> getTopologies() {
        return topologies;
    }

    /**
     * @param topologies the topologies to set
     */
    public void setTopologies(Map<String, TopologyType> topologies) {
        this.topologies = topologies;
    }
}
