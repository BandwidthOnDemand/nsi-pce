package net.es.nsi.pce.config.topo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.config.FileBasedConfigProvider;
import net.es.nsi.pce.config.topo.nml.BidirectionalEthernetPort;
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.config.topo.nml.Relationships;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.topology.jaxb.BidirectionalPortType;
import net.es.nsi.pce.topology.jaxb.LabelGroupType;
import net.es.nsi.pce.topology.jaxb.LabelType;
import net.es.nsi.pce.topology.jaxb.NSAType;
import net.es.nsi.pce.topology.jaxb.NetworkObject;
import net.es.nsi.pce.topology.jaxb.PortGroupRelationType;
import net.es.nsi.pce.topology.jaxb.PortGroupType;
import net.es.nsi.pce.topology.jaxb.PortRelationType;
import net.es.nsi.pce.topology.jaxb.PortType;
import net.es.nsi.pce.topology.jaxb.TopologyRelationType;
import net.es.nsi.pce.topology.jaxb.TopologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file based provider that reads XML formatted NML topology files and
 * creates simple network objects used to later build NSI topology.  Each
 * instance of the class models one NSA document in NML.
 * 
 * @author hacksaw
 */
public class NmlTopologyFile extends FileBasedConfigProvider implements TopologyProvider {
    private static final Logger log = LoggerFactory.getLogger(NmlTopologyFile.class);
    
    // Keep the original NML NSA entry.
    private NSAType nsa;

    // Map holding the network topologies indexed by network Id.
    private ConcurrentHashMap<String, TopologyType> topologies = new ConcurrentHashMap<String, TopologyType>();
    
    // Map holding the network topologies indexed by network Id.
    private ConcurrentHashMap<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<String, EthernetPort>();
    
    @Override
    public void loadConfig() throws Exception {
        // Check to see if file associated with this topology has changed.
        File configFile = new File(this.getFilename()); 
        if (!configFile.exists()) {
            throw new FileNotFoundException("Topology file does not exist: " + configFile);
        }
        
        String ap = configFile.getAbsolutePath();
        log.info("Processing NML topology file " + ap);

        if (!isFileUpdated()) {
            log.info("No change, file already loaded: " + ap);
            return;
        }
        
        // Looks like we have a change and need to process.
        log.debug("File change detected, loading " + ap);

        try {
            nsa = NmlParser.getInstance().parseNSA(ap);
            log.info("Loaded topology for NSA " + nsa.getId());
        }
        catch (JAXBException | FileNotFoundException jaxb) {
            log.error("Error parsing file: " + ap, jaxb);
            throw jaxb;
        }

        // Populate temportary maps and replace once completed.
        ConcurrentHashMap<String, TopologyType> newTopologies = new ConcurrentHashMap<String, TopologyType>();
        ConcurrentHashMap<String, EthernetPort> newEthernetPorts = new ConcurrentHashMap<String, EthernetPort>();
        
        // Parse the NSA object into component parts.
        for (TopologyType topology : nsa.getTopology()) {
            // Save the topology.
            newTopologies.put(topology.getId(), topology);

            // Unidirectional ports are modelled using Relations.
            for (TopologyRelationType relation : topology.getRelation()) {
                // Assume these are all Ethernet ports in topology for now.
                Orientation orientation = Orientation.inboundOutbound;
                if (relation.getType().equalsIgnoreCase(Relationships.hasOutboundPort)) {
                     orientation = Orientation.outbound;
                }
                else if (relation.getType().equalsIgnoreCase(Relationships.hasInboundPort)) {
                    orientation = Orientation.inbound;
                }

                // Some topologies use PortGroup to model unidirectional ports.
                for (PortGroupType portGroup : relation.getPortGroup()) {
                    log.trace("Creating unidirectional Ethernet port: " + portGroup.getId());
                    EthernetPort ethPort = new EthernetPort();
                    ethPort.setNsaId(nsa.getId());
                    ethPort.setTopologyId(topology.getId());
                    ethPort.setPortId(portGroup.getId());
                    ethPort.setOrientation(orientation);
                    ethPort.setDirectionality(Directionality.unidirectional);

                    // Extract the labels associated with this port and create individual VLAN labels.
                    ethPort.setLabelGroups(portGroup.getLabelGroup());
                    for (LabelGroupType labelGroup : portGroup.getLabelGroup()) {
                        // We break out the vlan specific lable.
                        if (EthernetPort.isVlanLabel(labelGroup.getLabeltype())) {
                            ethPort.addVlanFromString(labelGroup.getValue());
                        }
                    }

                    // PortGroup relationship has isAlias connection information.
                    int count = 0;
                    for (PortGroupRelationType pgRelation : portGroup.getRelation()) {
                        log.trace("Looking for isAlias relationship: " + pgRelation.getType());
                        if (Relationships.isAlias(pgRelation.getType())) {
                            log.trace("Found isAlias relationship.");
                            for (PortGroupType alias : pgRelation.getPortGroup()) {
                                log.trace("isAlias: " + alias.getId());
                                ethPort.getConnectedTo().add(alias.getId());
                                count++;
                            }
                        }
                    }

                    if (count == 0) {
                        log.error("Unidirectional port " + ethPort.getPortId() + " has zero isAlias relationships.");
                    }
                    else if (count > 1) {
                        log.error("Unidirectional port " + ethPort.getPortId() + " has " + count + "isAlias relationships.");
                    }

                    newEthernetPorts.put(portGroup.getId(), ethPort);
                }

                // Some topologies use Port to model unidirectional ports.
                for (PortType port : relation.getPort()) {
                    log.trace("Creating unidirectional Ethernet port: " + port.getId());
                    EthernetPort ethPort = new EthernetPort();
                    ethPort.setNsaId(nsa.getId());
                    ethPort.setTopologyId(topology.getId());
                    ethPort.setPortId(port.getId());
                    ethPort.setOrientation(orientation);
                    ethPort.setDirectionality(Directionality.unidirectional);

                    // Extract the label associated with this port and create the VLAN.
                    LabelType label = port.getLabel();
                    ethPort.getLabels().add(label);
                    if (EthernetPort.isVlanLabel(label.getLabeltype())) {
                        ethPort.addVlanFromString(label.getValue());
                    }

                    // Port relationship has isAlias connection information.
                    int count = 0;
                    for (PortRelationType pRelation : port.getRelation()) {
                        log.trace("Looking for isAlias relationship: " + pRelation.getType());
                        if (Relationships.isAlias(pRelation.getType())) {
                            log.trace("Found isAlias relationship.");
                            for (PortType alias : pRelation.getPort()) {
                                log.trace("isAlias: " + alias.getId());
                                ethPort.getConnectedTo().add(alias.getId());
                                count++;
                            }
                        }
                    }

                    if (count == 0) {
                        log.error("Unidirectional port " + ethPort.getPortId() + " has zero isAlias relationships.");
                    }
                    else if (count > 1) {
                        log.error("Unidirectional port " + ethPort.getPortId() + " has " + count + "isAlias relationships.");
                    }

                    newEthernetPorts.put(port.getId(), ethPort);
                }                    
            }

            // Bidirectional ports are stored in group element.
            List<NetworkObject> groups = topology.getGroup();
            for (NetworkObject group : groups) {
                log.trace("group object id: " + group.getId());

                // Process the BidirectionalPort.
                if (group instanceof BidirectionalPortType) {
                    log.trace("group object is BidirectionalPortType");
                    BidirectionalPortType port = (BidirectionalPortType) group;

                    BidirectionalEthernetPort ethPort = new BidirectionalEthernetPort();
                    ethPort.setNsaId(nsa.getId());
                    ethPort.setTopologyId(topology.getId());
                    ethPort.setPortId(port.getId());
                    ethPort.setOrientation(Orientation.inboundOutbound);
                    ethPort.setDirectionality(Directionality.bidirectional);

                    // Process port groups containing the unidirectional references.
                    List<JAXBElement<? extends NetworkObject>> rest = port.getRest();
                    for (JAXBElement<?> element: rest) {
                        if (element.getValue() instanceof PortGroupType) {
                            PortGroupType pg = (PortGroupType) element.getValue();
                            log.trace("Unidirectional port: " + pg.getId());
                            EthernetPort uniPort = newEthernetPorts.get(pg.getId());
                            if (uniPort == null) {
                                log.error("Bidirectional port " + port.getId() + " has no correctponding unidirectional port entry.  Dropping from topology!");
                                continue;
                            }

                            if (uniPort.getOrientation() == Orientation.inbound) {
                                ethPort.setInbound(uniPort);                                    
                            }
                            else if (uniPort.getOrientation() == Orientation.outbound) {
                                ethPort.setOutbound(uniPort);
                            }
                            else {
                                log.error("Bidirectional port " + port.getId() + " has an invalid relation reference for " + uniPort.getPortId());
                                throw new NoSuchElementException("Bidirectional port " + port.getId() + " has an invalid relation reference for " + uniPort.getPortId());
                            }
                        }
                        else if (element.getValue() instanceof PortType) {
                            PortType p = (PortType) element.getValue();
                            log.trace("Unidirectional port: " + p.getId());
                            EthernetPort uniPort = newEthernetPorts.get(p.getId());
                            if (uniPort == null) {
                                log.error("Bidirectional port " + port.getId() + " has no correctponding unidirectional port entry.");
                                throw new NoSuchElementException("Bidirectional port " + port.getId() + " has no correctponding unidirectional port entry.");
                            }

                            if (uniPort.getOrientation() == Orientation.inbound) {
                                ethPort.setInbound(uniPort);                                    
                            }
                            else if (uniPort.getOrientation() == Orientation.outbound) {
                                ethPort.setOutbound(uniPort);
                            }
                            else {
                                log.error("Bidirectional port " + port.getId() + " has an invalid relation reference for " + uniPort.getPortId());
                                throw new NoSuchElementException("Bidirectional port " + port.getId() + " has an invalid relation reference for " + uniPort.getPortId());
                            }                                
                        }
                    }

                    if (ethPort.getInbound() == null) {
                        log.error("Bidirectional port " + port.getId() + " does not have an associated inbound unidirectional port. Dropping from topology!");
                        continue; 
                    }
                    else if (ethPort.getOutbound() == null) {
                        log.error("Bidirectional port " + port.getId() + " does not have an associated outbound unidirectional port.  Dropping from topology!");
                        continue;
                    }

                    // Merge the VLAN id from uni ports into bidirectional port.
                    Set<Integer> inVLANs = ethPort.getInbound().getVlans();
                    Set<Integer> outVLANs = ethPort.getOutbound().getVlans();

                    if (inVLANs == null && outVLANs == null) {
                        // No vlans provided so we can assume this is okay.
                        log.trace("Bidirectional port " + port.getId() + " has vlans are null for " + ethPort.getInbound().getPortId() + ", and " + ethPort.getOutbound().getPortId());
                    }
                    if (inVLANs != null && outVLANs != null && inVLANs.equals(outVLANs)) {
                        ethPort.setVlans(inVLANs);
                    }
                    else {
                        log.error("Bidirectional port " + port.getId() + " contains unidirectional ports with differing vlan ranges.");
                        throw new IllegalArgumentException("Bidirectional port " + port.getId() + " contains unidirectional ports with differing vlan ranges.");                            
                    }

                    newEthernetPorts.put(port.getId(), ethPort);
                }
            }
        }
        
        // We are done so update the existing topology with this new one.
        topologies = newTopologies;
        ethernetPorts = newEthernetPorts;        
    }

    @Override
    public Topology getTopology() throws Exception {
        loadConfig();
        return null;
    }

    @Override
    public Set<String> getNetworkIds() {
        return topologies.keySet();
    }

    @Override
    public void setTopologySource(String source) {
        this.setFilename(source);
    }
    
    @Override
    public String getTopologySource() {
        return this.getFilename();
    }
        
    @Override
    public void loadTopology() throws Exception {
        this.loadConfig();
    }

    /**
     * @return the NSA managing this topology.
     */
    public NSAType getNsa() {
        return nsa;
    }

    /**
     * @param topologies the topologies to set
     */
    public void setNsa(NSAType nsa) {
        this.nsa = nsa;
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
        this.topologies.clear();
        this.topologies.putAll(topologies);
    }

    /**
     * @return the ethernetPorts
     */
    public Map<String, EthernetPort> getEthernetPorts() {
        return ethernetPorts;
    }

    /**
     * @param ethernetPorts the ethernetPorts to set
     */
    public void setEthernetPorts(Map<String, EthernetPort> ethernetPorts) {
        this.ethernetPorts.clear();
        this.ethernetPorts.putAll(ethernetPorts);
    }
}
