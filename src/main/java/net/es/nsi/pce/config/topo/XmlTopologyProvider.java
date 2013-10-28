package net.es.nsi.pce.config.topo;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.config.FileBasedConfigProvider;
import net.es.nsi.pce.config.topo.nml.BidirectionalEthernetPort;
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Nsa;
import net.es.nsi.pce.pf.api.topo.Sdp;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.topology.jaxb.NSAType;
import net.es.nsi.pce.topology.jaxb.TopologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

        
/**
 * This class models an XML based topology provider, or more specifically,
 * manages NSI topology derived from XML-based NML topology specifications.
 * It creates instances of NmlTopologyFile providers (one for each network)
 * and then consolidates the NML topology into a simple NSI-based topology.
 * 
 * @author hacksaw
 */
public class XmlTopologyProvider extends FileBasedConfigProvider implements TopologyProvider, InitializingBean {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Location of topology files to load and monitor.
    private String sourceDirectory;
    
    // Topology files indexed by file name.
    private Map<String, NmlTopologyFile> topologyFileMap = new ConcurrentHashMap<String, NmlTopologyFile>();

    // Map holding the network topologies indexed by network Id.
    private Map<String, NSAType> nsas = new ConcurrentHashMap<String, NSAType>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, TopologyType> topologies = new ConcurrentHashMap<String, TopologyType>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<String, EthernetPort>();    

    private Topology nsiTopology = new Topology();
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Date date;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        this.loadTopology();
    }
    
    /**
     * @return the sourceDirectory
     */
    public String getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * @param sourceDirectory the sourceDirectory to set
     */
    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }
        
    @Override
    public void loadConfig() throws JAXBException, FileNotFoundException {
        if (log.isTraceEnabled()) {
            date = new Date();
            log.trace("----------------------------------------------------------");
            log.trace("XmlTopologyProvider.loadConfig(): Starting " + dateFormat.format(date));
        }
        
        // Get a list of files for supplied directory.
        File folder = new File(sourceDirectory);
        File[] listOfFiles = folder.listFiles(); 

        String apSourceDirectory = folder.getAbsolutePath();
        log.info("Loading topology information from directory " + apSourceDirectory);
 
        // TODO: Look for changes in files and only process if we really need to....
        
        // Clear our existing file map so we can start freash.
        topologyFileMap.clear();

        // Load each file from supplied directory.
        String file;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                file = listOfFiles[i].getAbsolutePath();
                if (file.endsWith(".xml") || file.endsWith(".xml")) {
                    NmlTopologyFile tFile = new NmlTopologyFile();
                    tFile.setTopologySource(file);
                    try {
                        tFile.loadConfig();
                    }
                    catch (Exception ex) {
                        log.error("loadTopology: Failed to load topology file: " + file, ex);
                        continue;
                    }
                    
                    topologyFileMap.put(file, tFile);
                }
            }
        }
        
        // TODO: use temp maps and swap when completed.
        
        // Consolidate individual topologies in one master list.
        nsas.clear();
        topologies.clear();
        ethernetPorts.clear();
        for (NmlTopologyFile nml : topologyFileMap.values()) {
            NSAType nsa = nml.getNsa();
            if (nsa.getId() == null || nsa.getId().isEmpty()) {
                log.error("Topology file missing NSA identifier: " + nml.getTopologySource());
                continue;
            }
            
            nsas.put(nsa.getId(), nsa);
            topologies.putAll(nml.getTopologies());
            ethernetPorts.putAll(nml.getEthernetPorts());
        }
        
        // Interconnect bidirectional ports using their unidirectional components.
        for (EthernetPort ethPort : ethernetPorts.values()) {
            if (ethPort.isBidirectional() && ethPort instanceof BidirectionalEthernetPort) {
                log.debug("Consolidating bidirectional link: " + ethPort.getPortId());
                BidirectionalEthernetPort biPort = (BidirectionalEthernetPort) ethPort;
                
                // Unidirectional links are in Bidirectional port definition.
                EthernetPort inbound = biPort.getInbound();
                EthernetPort outbound = biPort.getOutbound();
                
                // Verify the inbound peer unidirectional ports exist and is connected.
                EthernetPort remoteOutbound = null;
                if (inbound.getConnectedTo().isEmpty()) {
                    log.trace("Bidirectional port " + biPort.getPortId() + " has no connectedTo information for inbound port: " + inbound.getPortId());
                }
                else {
                    // Assume only one for now.
                    String remotePortId = inbound.getConnectedTo().get(0);
                    remoteOutbound = ethernetPorts.get(remotePortId);
                
                    if (remoteOutbound == null) {
                        log.error("Bidirectional port " + biPort.getPortId() + " has inbound unidirectional port" + inbound.getPortId() + " with bad remote reference " + remotePortId);
                    }
                }
                
                // Verify the outbound peer unidirectional ports exist.
                EthernetPort remoteInbound = null;
                if (outbound.getConnectedTo().isEmpty()) {
                    log.trace("Bidirectional port " + biPort.getPortId() + " has no connectedTo information for inbound port: " + outbound.getPortId());
                }
                else {
                    // Assume only one for now.
                    String remotePortId = outbound.getConnectedTo().get(0);
                    remoteInbound = ethernetPorts.get(remotePortId);

                    if (remoteInbound == null) {
                        log.trace("Bidirectional port " + biPort.getPortId() + " has outbound unidirectional port" + outbound.getPortId() + " with bad remote reference " + remotePortId);
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
                        BidirectionalEthernetPort remotePort = findBidirectionalEthernetPortByMemberId(remoteInbound.getPortId(), remoteOutbound.getPortId());
                        if (remotePort == null) {
                            log.error("BidirectionalPort " + biPort + " has no compatible topology peer.");
                        }
                        else {
                            log.trace("Consolidating bidirectional ports: " + biPort.getPortId() + " and " + remotePort.getPortId());
                            biPort.getConnectedTo().add(remotePort.getPortId());
                            biPort.setRemotePort(remotePort);
                        }
                    }
                }
            }
        }
        
        if (log.isTraceEnabled()) {
            date = new Date();
            log.trace("XmlTopologyProvider.loadConfig(): Ending " + dateFormat.format(date));
            log.trace("----------------------------------------------------------");
        }
    }
   
    private BidirectionalEthernetPort findBidirectionalEthernetPortByMemberId(String inbound, String outbound) {
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
    public void loadTopology() throws Exception {
        if (log.isDebugEnabled()) {
            date = new Date();
            log.debug("----------------------------------------------------------");
            log.debug("XmlTopologyProvider.loadTopology(): Starting " + dateFormat.format(date));
        }
        
        // Load the NML topology model.
        loadConfig();

        // Create the NSI STP network topology.
        nsiTopology.clear();
        
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
                nsiTopology.addNetwork(net);                
            }
            
            nsiTopology.addNsa(nsa);
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
                Network localNetwork = nsiTopology.getNetworkById(localBiPort.getTopologyId());
                
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
                Network remoteNetwork = null;
                if (remoteBiPort != null) {
                    // Remove remote port matching the local port so we do not
                    // process it twice.
                    ethPorts.remove(remoteBiPort.getPortId());
                    remoteNetwork = nsiTopology.getNetworkById(remoteBiPort.getTopologyId());
                    
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
                            nsiTopology.addSdp(sdp);
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
            log.debug("XmlTopologyProvider.loadTopology(): Ending " + dateFormat.format(date));
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
        this.setSourceDirectory(source);
    }

    @Override
    public String getTopologySource() {
        return this.getSourceDirectory();
    }
}
