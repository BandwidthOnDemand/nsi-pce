/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.config.FileBasedConfigProvider;
import net.es.nsi.pce.config.topo.nml.BidirectionalEthernetPort;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Sdp;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.topology.jaxb.TopologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

        
/**
 *
 * @author hacksaw
 */
public class XmlTopologyProvider extends FileBasedConfigProvider implements TopologyProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Location of topology files to load and monitor.
    private String sourceDirectory;
    
    // Topology files indexed by file name.
    private Map<String, NmlTopologyFile> topologyFileMap = new ConcurrentHashMap<String, NmlTopologyFile>();

    // Map holding the network topologies indexed by network Id.
    private Map<String, TopologyType> topologies = new ConcurrentHashMap<String, TopologyType>();
    
    // Map holding the network topologies indexed by network Id.
    private Map<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<String, EthernetPort>();    

    private Topology nsiTopology = new Topology();
            
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
 
        // Get a list of files for supplied directory.
        File folder = new File(sourceDirectory);
        File[] listOfFiles = folder.listFiles(); 

        String apSourceDirectory = folder.getAbsolutePath();
        log.info("Loading topology information from directory " + apSourceDirectory);
 
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
                
        // Consolidate individual topologies in one master list.
        topologies.clear();
        ethernetPorts.clear();
        for (Entry<String, NmlTopologyFile> entry : topologyFileMap.entrySet()) {
            topologies.putAll(entry.getValue().getTopologies());
            ethernetPorts.putAll(entry.getValue().getEthernetPorts());
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
                    log.debug("Bidirectional port " + biPort.getPortId() + " has no connectedTo information for inbound port: " + inbound.getPortId());
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
                    log.debug("Bidirectional port " + biPort.getPortId() + " has no connectedTo information for inbound port: " + outbound.getPortId());
                }
                else {
                    // Assume only one for now.
                    String remotePortId = outbound.getConnectedTo().get(0);
                    remoteInbound = ethernetPorts.get(remotePortId);

                    if (remoteInbound == null) {
                        log.error("Bidirectional port " + biPort.getPortId() + " has outbound unidirectional port" + outbound.getPortId() + " with bad remote reference " + remotePortId);
                    }
                }
                
                // We need valid connectivity information before we can consolidate links.
                if (remoteOutbound == null || remoteInbound == null) {
                    log.error("Bidirectional port " + biPort.getPortId() + " cannot be consolidated due to missing port reference.");
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
                            log.debug("Consolidating bidirectional ports: " + biPort.getPortId() + " and " + remotePort.getPortId());
                            biPort.getConnectedTo().add(remotePort.getPortId());
                            biPort.setRemotePort(remotePort);
                        }
                    }
                }
            }
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
    
    @Override
    public void loadTopology() throws Exception {
        // Load the NML topology model.
        loadConfig();

        // Create the NSI STP network topology.
        nsiTopology.clear();
        
        // Convert each NML Topology element to an NSI Network object.
        for (String networkId : topologies.keySet()) {
            log.debug("Converting NML topology for network: " + networkId);
            Network net = new Network();
            net.setNetworkId(networkId);
            nsiTopology.setNetwork(networkId, net);
        }

        // Add each bidirectional port the the STP topology.
        Map<String, EthernetPort> ethPorts = new ConcurrentHashMap<String, EthernetPort>(ethernetPorts);
        for (String key : ethPorts.keySet()) {
            EthernetPort localPort = ethPorts.remove(key);
            
            // Make sure we have not already processed the port.
            if (localPort != null && localPort.isBidirectional()) {
                log.debug("Converting Ethernet port to STP: " + localPort.getPortId());
                
                BidirectionalEthernetPort localBiPort = (BidirectionalEthernetPort) localPort;
                Network localNetwork = nsiTopology.getNetwork(localBiPort.getTopologyId());
                if (localNetwork == null) {
                    log.error("Could not find network " + localBiPort.getTopologyId());
                }
                
                // We only process remote end STP if one exists!
                BidirectionalEthernetPort remoteBiPort = localBiPort.getRemotePort();
                Network remoteNetwork = null;
                if (remoteBiPort != null) {
                    // Remove remote port matching the local port.
                    ethPorts.remove(remoteBiPort.getPortId());
                    remoteNetwork = nsiTopology.getNetwork(remoteBiPort.getTopologyId());
                }
                else {
                    log.error("No topology link for port " + localPort.getPortId());
                }
                
                // We create an STP per vlan.  We already verified vlans match on both ends.
                for (Integer vlanId : localBiPort.getVlans()) {
                    log.debug("Converting local port " + localBiPort.getPortId() + " vlan = " + vlanId);
                    
                    // Create the local STP and store in network topology.
                    Stp localStp = localNetwork.newStp(localBiPort, vlanId);
                    localNetwork.put(localStp.getId(), localStp);                    
                    
                    // If we have a remote port then create an STP, link them, and created associated SDP.
                    if (remoteBiPort != null && remoteNetwork != null) {
                        log.debug("Converting remote port " + remoteBiPort.getPortId() + " vlan = " + vlanId);
                        Stp remoteStp = remoteNetwork.newStp(remoteBiPort, vlanId);
                    
                        // Save a remote cross reference.
                        localStp.setRemoteStp(remoteStp);
                        remoteStp.setRemoteStp(localStp);
                    
                        // Store remote STP in associated network topology.
                        remoteNetwork.put(remoteStp.getId(), remoteStp);
                    
                        // Create the SDP links for local STP.
                        Sdp localSdp = new Sdp();
                        localSdp.setA(localStp);
                        localSdp.setZ(remoteStp);
                        localNetwork.getSdp().add(localSdp);
                    
                        // Create the SDP links for remote STP.
                        Sdp remoteSdp = new Sdp();
                        remoteSdp.setA(remoteStp);
                        remoteSdp.setZ(localStp);
                        remoteNetwork.getSdp().add(remoteSdp);
                    }
                }
            }
        }
    }
    
    @Override
    public Topology getTopology() throws Exception {
        loadTopology();
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
