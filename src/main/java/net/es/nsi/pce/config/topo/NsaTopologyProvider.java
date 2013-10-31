package net.es.nsi.pce.config.topo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import net.es.nsi.pce.config.topo.nml.BidirectionalEthernetPort;
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.config.topo.nml.Relationships;
import net.es.nsi.pce.jersey.RestClient;
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
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reads a remote XML formatted NML topology and creates simple
 * network objects used to later build NSI topology.  Each instance of the class
 * models a single NSA in NML.
 * 
 * @author hacksaw
 */
public class NsaTopologyProvider implements TopologyProvider {
    private static final Logger log = LoggerFactory.getLogger(NsaTopologyProvider.class);
    
    // The remote location of the file to read.
    private String target = null;
    
    // Time we last read the NSA topology.
    private Date lastModified = new Date(0L);
    
    // Keep the original NML NSA entry.
    private NSAType nsa = null;

    // Map holding the network topologies indexed by network Id.
    private ConcurrentHashMap<String, TopologyType> topologies = new ConcurrentHashMap<String, TopologyType>();
    
    // Map holding the network topologies indexed by network Id.
    private ConcurrentHashMap<String, EthernetPort> ethernetPorts = new ConcurrentHashMap<String, EthernetPort>();
    
    /**
     * Default class constructor.
     */
    public NsaTopologyProvider() {}
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public NsaTopologyProvider(String target) {
        this.target = target;
    }

    /**
     * Returns the configured remote topology endpoint.
     * 
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the remote topology endpoint.
     * 
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * NSA topology document was modified.
     * 
     * @return the lastModified date of the remote topology document.
     */
    public Date getLastModified() {
        return new Date(lastModified.getTime());
    }

    /**
     * Set the last modified date of the cached remote topology document.
     * 
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }    
    
    /**
     * Read the NML topology from target location using HTTP GET operation.
     * 
     * @return The JAXB NSA element from the NML topology.
     */
    private NSAType readNsaTopology() throws Exception {
        // Use the REST client to retrieve the master topology as a string.
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webGet = client.target(getTarget());
        Response response = webGet.request(MediaType.APPLICATION_XML) .header("If-Modified-Since", DateUtils.formatDate(getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        
        // A 304 Not Modified indicates we already have a up-to-date document.
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            return null;
        }
        
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.error("readNsaTopology: Failed to retrieve NSA topology " + getTarget());
            throw new NotFoundException("Failed to retrieve NSA topology " + getTarget());
        }
        
        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        if (lastMod != null) {
            log.debug("readNsaTopology: Updating last modified time " + DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123));
            setLastModified(lastMod);
        }

        // Now we want the NML XML document.
        String xml = response.readEntity(String.class);
        
        // Parse the NSA topology. 
        NSAType topology = XmlParser.getInstance().parseNsaFromString(xml);
        
        return topology;
    }
    
    /**
     * Returns a current version of the NSA topology, retrieving a new
     * version from the remote endpoint if available.
     * 
     * @return Master topology.
     * @throws Exception If an error occurs when reading remote topology.
     */
    private synchronized void loadNsaTopology() throws Exception {
        
        NSAType newNsa = this.readNsaTopology();
        if (newNsa != null && nsa == null) {
            // We don't have a previous version so update with this version.
            nsa = newNsa;
        }
        else if (newNsa != null && nsa != null) {
            // Only update if this version is newer.
            if (newNsa.getVersion() == null || nsa.getVersion() == null) {
                // Missing version information so we have to assume an update.
                nsa = newNsa;
            }
            else if (newNsa.getVersion().compare(nsa.getVersion()) == DatatypeConstants.GREATER) {
                nsa = newNsa;
            }
        }
    }
    
    /**
     * Returns a current version of the NSA topology only if a new version
     * was available from the remote endpoint if available.
     * 
     * @return
     * @throws Exception 
     */
    private NSAType getNsaTopologyIfModified() throws Exception {
        NSAType oldNsa = nsa;
        loadNsaTopology();
        NSAType newNsa = nsa;

        if (newNsa != null && oldNsa == null) {
            // We don't have a previous version so there is a change.
            return nsa;
        }
        else if (newNsa != null && oldNsa != null) {
            // Only update if this version is newer.
            if (newNsa.getVersion().compare(oldNsa.getVersion()) == DatatypeConstants.GREATER) {
                return nsa;
            }
        }
        
        // There must not have been a change.
        return null;
    }
    
    private synchronized void load() throws Exception {
        log.info("Processing NML topology " + getTarget());

        if (getNsaTopologyIfModified() == null) {
            log.info("No topology change, NSA topology already loaded: " + getTarget() + ", version=" + nsa.getVersion());
            return;
        }
        
        // Looks like we have a change and need to process.
        log.info("Topology change detected, loading new version of " + nsa.getId());

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
        load();
        return null;
    }

    @Override
    public Set<String> getNetworkIds() {
        return topologies.keySet();
    }

    @Override
    public void setTopologySource(String source) {
        this.setTarget(source);
    }
    
    @Override
    public String getTopologySource() {
        return this.getTarget();
    }
        
    @Override
    public void loadTopology() throws Exception {
        this.load();
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
    
    /**
     * @return the auditInterval
     */
    @Override
    public long getAuditInterval() {
        return 10*60*1000;
    }
}
