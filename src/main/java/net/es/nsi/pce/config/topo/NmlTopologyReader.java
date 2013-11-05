/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import net.es.nsi.pce.config.topo.nml.BidirectionalEthernetPort;
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.config.topo.nml.Relationships;
import net.es.nsi.pce.pf.api.topo.TopologyReader;
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
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
public abstract class NmlTopologyReader implements TopologyReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // The remote location of the file to read.
    private String target = null;
    
    // Time we last read the NSA topology.
    private long lastModified = 0L;
    
    // Keep the original NML NSA entry.
    private NSAType nsa = null;

    // Map holding the network topologies indexed by network Id.
    private ConcurrentHashMap<String, TopologyType> topologies = new ConcurrentHashMap<>();
    
    // Map holding the network topologies indexed by network Id.
    private ConcurrentHashMap<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<>();
    
    /**
     * Returns the configured remote topology endpoint.
     * 
     * @return the target
     */
    @Override
    public String getTarget() {
        return target;
    }

    /**
     * Sets the remote topology endpoint.
     * 
     * @param target the target to set
     */
    @Override
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * NSA topology document was modified.
     * 
     * @return the lastModified date of the remote topology document.
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Set the last modified date of the cached remote topology document.
     * 
     * @param lastModified the lastModified to set
     */
    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * @return the NSA managing this topology.
     */
    @Override
    public NSAType getNsa() {
        return nsa;
    }

    @Override
    public void setNsa(NSAType nsa) {
        this.nsa = nsa;
    }
        
    /**
     * @return the topologies
     */
    @Override
    public Map<String, TopologyType> getTopologies() {
        return Collections.unmodifiableMap(topologies);
    }
    
    public void setTopologies(ConcurrentHashMap<String, TopologyType> topologies) {
        this.topologies.clear();
        this.topologies.putAll(topologies);
    }
    
    /**
     * @return the ethernetPorts
     */
    @Override
    public Map<String, EthernetPort> getEthernetPorts() {
        return Collections.unmodifiableMap(ethernetPorts);
    }

    public void setEthernetPorts(ConcurrentHashMap<String, EthernetPort> ethernetPorts) {
        this.ethernetPorts.clear();
        this.ethernetPorts.putAll(ethernetPorts);
    }
    
    public synchronized void processUpdate() throws Exception {
        log.info("Processing NML topology update for " + getTarget());

        // Populate temportary maps and replace once completed.
        ConcurrentHashMap<String, TopologyType> newTopologies = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, EthernetPort> newEthernetPorts = new ConcurrentHashMap<>();
        
        // Parse the NSA object into component parts.
        for (TopologyType topology : getNsa().getTopology()) {
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
                    ethPort.setNsaId(getNsa().getId());
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
                    ethPort.setNsaId(this.getNsa().getId());
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
                    ethPort.setNsaId(this.getNsa().getId());
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
        setTopologies(newTopologies);
        setEthernetPorts(newEthernetPorts);        
    } 
    
    @Override
    public synchronized void load() throws Exception {
        log.info("Processing NML topology " + getTarget());

        if (getNsaTopologyIfModified() == null) {
            log.info("No topology change, NSA topology already loaded: " + getTarget() + ", version=" + getNsa().getVersion());
            return;
        }
                
        // Looks like we have a change and need to process.
        log.info("Topology change detected, loading new version of " + getNsa().getId());
        
        this.processUpdate();
    }
    
    
    /**
     * Returns a current version of the NSA topology, retrieving a new
     * version from the remote endpoint if available.
     * 
     * @return Master topology.
     * @throws Exception If an error occurs when reading remote topology.
     */
    private synchronized void loadNsaTopology() throws Exception {
        NSAType newNsa = readNsaTopology();
        
        if (newNsa == null) {
            log.debug("loadNsaTopology: returned topology null.");
            return;
        }
        
        if (getNsa() == null) {
            // We don't have a previous version so update with this version.
            this.setNsa(newNsa);
            log.debug("loadNsaTopology: no old topology so accepting new " + newNsa.getId());
        }
        else {
            // Only update if this version is newer.
            if (newNsa.getVersion() == null || this.getNsa().getVersion() == null) {
                // Missing version information so we have to assume an update.
                this.setNsa(newNsa);
                log.debug("loadNsaTopology: no version number in topology so accepting new " + newNsa.getId());
            }
            else if (newNsa.getVersion().compare(this.getNsa().getVersion()) == DatatypeConstants.GREATER) {
                this.setNsa(newNsa);
                log.debug("loadNsaTopology: version number indicates topology update so accepting new " + newNsa.getId());
            }
        }
    }
    
    /**
     * Returns a current version of the NSA topology only if a new version
     * was available from the remote endpoint.
     * 
     * @return
     * @throws Exception 
     */
    private NSAType getNsaTopologyIfModified() throws Exception {
        NSAType oldNsa = getNsa();
        loadNsaTopology();
        NSAType newNsa = getNsa();

        if (newNsa == null) {
            log.debug("getNsaTopologyIfModified: newNSA is null");
            return null;
        }
        else if (oldNsa == null) {
            log.debug("getNsaTopologyIfModified: oldNsa is null");
            return newNsa;
        }
        else if (newNsa.getVersion().compare(oldNsa.getVersion()) == DatatypeConstants.GREATER) {
            log.debug("getNsaTopologyIfModified: newNSA is an update");
            return newNsa;
        }
        
        // There must not have been a change.
        return null;
    }
}
