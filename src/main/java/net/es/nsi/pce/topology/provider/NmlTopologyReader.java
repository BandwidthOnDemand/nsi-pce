/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.config.topo.nml.Relationships;
import net.es.nsi.pce.jersey.Utilities;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlBidirectionalPortType;
import net.es.nsi.pce.topology.jaxb.NmlLabelGroupType;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import net.es.nsi.pce.topology.jaxb.NmlNSAType;
import net.es.nsi.pce.topology.jaxb.NmlNetworkObject;
import net.es.nsi.pce.topology.jaxb.NmlPortGroupRelationType;
import net.es.nsi.pce.topology.jaxb.NmlPortGroupType;
import net.es.nsi.pce.topology.jaxb.NmlPortRelationType;
import net.es.nsi.pce.topology.jaxb.NmlPortType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceRelationType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceType;
import net.es.nsi.pce.topology.jaxb.NmlTopologyRelationType;
import net.es.nsi.pce.topology.jaxb.NmlTopologyType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDefinitionType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiNetworkFactory;
import net.es.nsi.pce.topology.model.NsiNsaFactory;
import net.es.nsi.pce.topology.model.NsiServiceFactory;
import net.es.nsi.pce.topology.model.NsiServiceDomainFactory;
import net.es.nsi.pce.topology.model.NsiStpFactory;
import net.es.nsi.pce.topology.model.NsiTopology;
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
    
    // If-Modified-Since value as retruned from remote server for NSA topology.
    private long lastModified = 0L;
    
    // Time we last discovered an NSA topology change.
    private long lastDiscovered = 0L;
    
    // Keep the original NML NSA entry.
    private NmlNSAType nsa = null;
    
    private NsiTopology nsiTopology = new NsiTopology();
     
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
     * @return the lastDiscovered
     */
    public long getLastDiscovered() {
        return lastDiscovered;
    }

    /**
     * @param lastDiscovered the lastDiscovered to set
     */
    public void setLastDiscovered(long lastDiscovered) {
        this.lastDiscovered = lastDiscovered;
    }
    
    /**
     * @return the NSA managing this topology.
     */
    @Override
    public NmlNSAType getNsa() {
        return nsa;
    }

    @Override
    public void setNsa(NmlNSAType nsa) {
        this.nsa = nsa;
    }

    /**
     * @return the nsiTopology
     */
    @Override
    public NsiTopology getNsiTopology() {
        return nsiTopology;
    }

    /**
     * @param nsiTopology the nsiTopology to set
     */
    public void setNsiTopology(NsiTopology nsiTopology) {
        this.nsiTopology = nsiTopology;
    }
    
    public synchronized void processUpdate() throws Exception {
        log.info("Processing NML topology update for " + getTarget());
        
        // Create the NSI topology.
        NsiTopology newNsiTopology = new NsiTopology();
 
        // Create the NSI NSA resource from NML.
        NsaType nsiNsa = NsiNsaFactory.createNsaType(getNsa());
        
        // Set discovered time to our last discovered value for the NML
        // document.  We may have seen this already, but we can't really
        // distinguish given only the complete NML document is versioned.
        nsiNsa.setDiscovered(Utilities.longToXMLGregorianCalendar(getLastDiscovered()));
        
        // Add it to the NSI topology.
        newNsiTopology.addNsa(nsiNsa);
            
        // Parse the NSA object into component parts.
        for (NmlTopologyType nmlTopology : getNsa().getTopology()) {        
            // Create a new NSI Network resource.
            NetworkType nsiNetwork = NsiNetworkFactory.createNetworkType(nmlTopology, nsiNsa);

            // Add this network resource to our NSI topology.
            newNsiTopology.addNetwork(nsiNetwork);
            
            // We need to build a reference to this new Network object for
            // use in the NSA object.
            ResourceRefType nsiNetworkRef = NsiNetworkFactory.createResourceRefType(nsiNetwork);
            nsiNsa.getNetwork().add(nsiNetworkRef);
            
            // Convert all Service Definitions to NSI Service resources.  The
            // Service Definitions elements are held in an ANY within the NML
            // document.
            Collection<ServiceType> nsiServices = getNsiServicesFromServiceDefinitions(nmlTopology.getAny(), nsiNetwork);
            newNsiTopology.addAllServices(nsiServices);
            
            // Add the Service references to the Network resource.
            for (ServiceType service : nsiServices) {
                ResourceRefType serviceRef = NsiServiceFactory.createResourceRefType(service);
                nsiNetwork.getService().add(serviceRef);
            }     
            
            // Unidirectional ports are modelled using Relations.
            Map<String, NmlPort> uniPortMap = getNmlPortsFromRelations(nmlTopology);

            // Bidirectional ports are stored in separate group element and
            // reference individual undirection Ports or PortGroups.
            Map<String, NmlPort> biPortMap = getNmlPortsFromBidirectionalPorts(nmlTopology, uniPortMap);
            
            Map<String, NmlPort> portMap = new HashMap<>();
            portMap.putAll(uniPortMap);
            portMap.putAll(biPortMap);
            
            // Now we convert the unidirectional NML Ports to STP.
            for (NmlPort port : uniPortMap.values()) {
                Collection<StpType> newStps = NsiStpFactory.createStps(port, newNsiTopology);
                newNsiTopology.addAllStp(newStps);
            }
            
            // Now we convert the bidirectional NML Ports to STP.
            for (NmlPort port : biPortMap.values()) {
                Collection<StpType> newStps = NsiStpFactory.createStps(port, newNsiTopology);
                newNsiTopology.addAllStp(newStps);
            }
            
            // Add the port references to the Network resource.
            for (StpType stp : newNsiTopology.getStps()) {
                ResourceRefType stpRef = NsiStpFactory.createResourceRefType(stp);
                nsiNetwork.getStp().add(stpRef);
            }

            // SwitchingServices are modelled using Relations.
            Collection<ServiceDomainType> nsiServiceDomains = getServiceDomainsFromSwitchingServices(nmlTopology, portMap, nsiNetwork, newNsiTopology);
            newNsiTopology.addAllServiceDomains(nsiServiceDomains);
            
            // Add the ServiceDomain references to the Network resource.
            for (ServiceDomainType sd : nsiServiceDomains) {
                ResourceRefType sdRef = NsiServiceDomainFactory.createResourceRefType(sd);
                nsiNetwork.getServiceDomain().add(sdRef);
            }            
        }
        
        // We are done so update the existing topology with this new one.
        setNsiTopology(newNsiTopology);
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

        // Strip off the subsecond component.  This is needed to avoid the
        // inaccuracies of the HTTP date fields during comparisons since they
        // are based on seconds.
        long currentTime = System.currentTimeMillis();
        this.setLastDiscovered(currentTime - currentTime % 1000);
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
        NmlNSAType newNsa = readNsaTopology();
        
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
    private NmlNSAType getNsaTopologyIfModified() throws Exception {
        NmlNSAType oldNsa = getNsa();
        loadNsaTopology();
        NmlNSAType newNsa = getNsa();

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
    
    private Collection<ServiceType> getNsiServicesFromServiceDefinitions(List<Object> any, NetworkType nsiNetwork) {
        ArrayList<ServiceType> services = new ArrayList<>();
        
        // Pull out the ServiceDefinition elements which are stored in ANY.
        for (Object object : any) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> jaxb = (JAXBElement) object;
                if (jaxb.getValue() instanceof ServiceDefinitionType) {
                    ServiceDefinitionType serviceDefinition = (ServiceDefinitionType) jaxb.getValue();

                    // We map Service Definitions to NSI Service resources.
                    ServiceType nsiService = NsiServiceFactory.createServiceType(serviceDefinition, nsiNetwork);
                    services.add(nsiService);
                }
            }
        }
        return services;
    }
    
    private Map<String, NmlPort> getNmlPortsFromRelations(NmlTopologyType nmlTopology) {
        Map<String, NmlPort> portMap = new HashMap<>();

        // Unidirectional ports and SwitchingService are modelled using Relations.
        for (NmlTopologyRelationType relation : nmlTopology.getRelation()) {                       
            if (relation.getType().equalsIgnoreCase(Relationships.hasOutboundPort) ||
                    relation.getType().equalsIgnoreCase(Relationships.hasInboundPort)) {

                // Determine the type of STP resource we will create.
                Orientation orientation = Orientation.inbound;
                if (relation.getType().equalsIgnoreCase(Relationships.hasOutboundPort)) {
                     orientation = Orientation.outbound;
                }

                // Some topologies use PortGroup to model a list of
                // unidirectional ports.
                for (NmlPortGroupType portGroup : relation.getPortGroup()) {
                    log.debug("Creating unidirectional STP for NML PortGroup: " + portGroup.getId());

                    // Extract the labels associated with this port.  We
                    // currently expect a single labelType with a range of
                    // values.  VLAN labels is all we currently know about.
                    Set<NmlLabelType> labels = new LinkedHashSet<>();
                    for (NmlLabelGroupType labelGroup : portGroup.getLabelGroup()) {
                        // We break out the vlan specific lable.
                        if (NmlEthernet.isVlanLabel(labelGroup.getLabeltype())) {
                            labels.addAll(NmlEthernet.labelGroupToLabels(labelGroup));
                        }
                    }

                    // PortGroup relationship has isAlias connection information.
                    String connectedTo = null;
                    int count = 0;
                    for (NmlPortGroupRelationType pgRelation : portGroup.getRelation()) {
                        log.debug("Looking for isAlias relationship: " + pgRelation.getType());
                        if (Relationships.isAlias(pgRelation.getType())) {
                            log.debug("Found isAlias relationship.");
                            for (NmlPortGroupType alias : pgRelation.getPortGroup()) {
                                log.debug("isAlias: " + alias.getId());
                                connectedTo = alias.getId();
                                count++;
                            }
                        }
                    }

                    // Log an error if we have more than one connectedTo
                    // relationship but continue.
                    if (count == 0) {
                        log.error("Unidirectional port " + portGroup.getId() + " has zero isAlias relationships.");
                    }
                    else if (count > 1) {
                        log.error("Unidirectional port " + portGroup.getId() + " has " + count + "isAlias relationships.");
                    }

                    // Store this port information in our scratch pad.
                    NmlPort nmlPort = new NmlPort();
                    nmlPort.setNsaId(getNsa().getId());
                    nmlPort.setTopologyId(nmlTopology.getId());
                    nmlPort.setId(portGroup.getId());
                    nmlPort.setName(portGroup.getName());
                    nmlPort.setOrientation(orientation);
                    nmlPort.setVersion(getNsa().getVersion());
                    nmlPort.setDiscovered(getLastDiscovered());
                    nmlPort.setLabels(labels);
                    nmlPort.setConnectedTo(connectedTo);
                    portMap.put(nmlPort.getId(), nmlPort);
                }

                // Some topologies use Port to model unidirectional ports.
                for (NmlPortType port : relation.getPort()) {
                    log.debug("Creating unidirectional STP for NML Port: " + port.getId());

                    // PortGroup relationship has isAlias connection information.
                    String connectedTo = null;
                    int count = 0;
                    for (NmlPortRelationType pgRelation : port.getRelation()) {
                        log.debug("Looking for isAlias relationship: " + pgRelation.getType());
                        if (Relationships.isAlias(pgRelation.getType())) {
                            log.debug("Found isAlias relationship.");
                            for (NmlPortType alias : pgRelation.getPort()) {
                                log.debug("isAlias: " + alias.getId());
                                connectedTo = alias.getId();
                                count++;
                            }
                        }
                    }

                    // Log an error if we have more than one connectedTo
                    // relationship but continue.
                    if (count == 0) {
                        log.error("Unidirectional port " + port.getId() + " has zero isAlias relationships.");
                    }
                    else if (count > 1) {
                        log.error("Unidirectional port " + port.getId() + " has " + count + "isAlias relationships.");
                    }

                    // Store this port information in our scratch pad.
                    NmlPort nmlPort = new NmlPort();
                    nmlPort.setNsaId(getNsa().getId());
                    nmlPort.setTopologyId(nmlTopology.getId());
                    nmlPort.setId(port.getId());
                    nmlPort.setName(port.getName());
                    nmlPort.setOrientation(orientation);
                    nmlPort.setVersion(getNsa().getVersion());
                    nmlPort.setDiscovered(getLastDiscovered());
                    LinkedHashSet<NmlLabelType> labels = new LinkedHashSet<>();
                    labels.add(port.getLabel());
                    nmlPort.setLabels(labels);
                    nmlPort.setConnectedTo(connectedTo);
                    portMap.put(nmlPort.getId(), nmlPort);
                }
            }
        }
        
        return portMap;
    }
    
    private Map<String, NmlPort> getNmlPortsFromBidirectionalPorts(NmlTopologyType nmlTopology, Map<String, NmlPort> uniPortMap) {
        Map<String, NmlPort> biPortMap = new HashMap<>();
        
        // Bidirectional ports are stored in separate group element and
        // reference individual undirection Ports or PortGroups.
        List<NmlNetworkObject> groups = nmlTopology.getGroup();
        for (NmlNetworkObject group : groups) {
            log.debug("NML NetworkObject id: " + group.getId());

            // Process the BidirectionalPort.  It will contain references
            // to either member Port or PortGroup elements.
            if (group instanceof NmlBidirectionalPortType) {
                log.debug("NML BidirectionalPortType: " + group.getId());
                NmlBidirectionalPortType port = (NmlBidirectionalPortType) group;

                // Process port groups containing the unidirectional references.
                NmlPort inbound = null;
                NmlPort outbound = null;
                List<Object> rest = port.getRest();
                for (Object obj: rest) {
                    if (obj instanceof JAXBElement) {
                        JAXBElement<?> element = (JAXBElement<?>) obj;
                        if (element.getValue() instanceof NmlPortGroupType) {
                            NmlPortGroupType pg = (NmlPortGroupType) element.getValue();
                            log.debug("NML Unidirectional port: " + pg.getId());

                            NmlPort tmp = uniPortMap.get(pg.getId());
                            if (tmp != null) {
                                if (tmp.getOrientation() == Orientation.inbound) {
                                    inbound = tmp;
                                }
                                else if (tmp.getOrientation() == Orientation.outbound) {
                                    outbound = tmp;
                                }
                                else {
                                    log.error("Bidirectional port " + port.getId() + " has invalid undirectional port reference (" + pg.getId());
                                }
                            }
                        }
                        else if (element.getValue() instanceof NmlPortType) {
                            NmlPortType p = (NmlPortType) element.getValue();
                            log.debug("Unidirectional port: " + p.getId());

                            NmlPort tmp = uniPortMap.get(p.getId());
                            if (tmp != null) {
                                if (tmp.getOrientation() == Orientation.inbound) {
                                    inbound = tmp;
                                }
                                else if (tmp.getOrientation() == Orientation.outbound) {
                                    outbound = tmp;
                                }
                                else {
                                    log.error("Bidirectional port " + port.getId() + " has invalid undirectional port reference (" + p.getId());
                                }
                            }                        
                        }
                    }
                }

                if (inbound == null) {
                    log.error("Bidirectional port " + port.getId() + " does not have an associated inbound unidirectional port. Dropping from topology!");
                    continue; 
                }
                else if (outbound == null) {
                    log.error("Bidirectional port " + port.getId() + " does not have an associated outbound unidirectional port.  Dropping from topology!");
                    continue;
                }

                // Merge the lables uni ports into bidirectional port.
                Set<NmlLabelType> inLabels = inbound.getLabels();
                Set<NmlLabelType> outLabels = outbound.getLabels();

                if (inLabels == null && outLabels == null) {
                    // No labels is a valid case.
                    log.debug("Bidirectional port " + port.getId() + " no lables for " + inbound.getId() + ", and " + outbound.getId());
                }
                else if (inLabels != null && outLabels != null) {
                    boolean consistent = true;
                    for (NmlLabelType inLabel : inLabels) {
                        boolean found = false;
                        for (NmlLabelType outLabel : outLabels) {
                            if (NsiStpFactory.labelEquals(inLabel, outLabel)) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            log.error("Bidirectional port " + port.getId() + " contains unidirectional ports with differing label ranges.  Dropping from topology!");
                            consistent = false;
                            break;
                        }
                    }

                    if (!consistent) {
                      continue;
                    }
                }
                else {
                    log.error("Bidirectional port " + port.getId() + " contains unidirectional ports with differing label ranges.");
                    //throw new IllegalArgumentException("Bidirectional port " + port.getId() + " contains unidirectional ports with differing vlan ranges.");                            
                    continue;
                }

                // Build the bidirection Port using unidirectional Labels
                // and store this port information in our scratch pad.
                NmlPort nmlPort = new NmlPort();
                nmlPort.setNsaId(getNsa().getId());
                nmlPort.setTopologyId(nmlTopology.getId());
                nmlPort.setId(port.getId());
                nmlPort.setName(port.getName());
                nmlPort.setOrientation(Orientation.inboundOutbound);
                nmlPort.setVersion(getNsa().getVersion());
                nmlPort.setDiscovered(getLastDiscovered());
                nmlPort.setLabels(inLabels);
                nmlPort.setInboundPort(inbound);
                nmlPort.setOutboundPort(outbound);
                biPortMap.put(nmlPort.getId(), nmlPort);
            }
        }
        
        return biPortMap;
    }
    
    private Collection<ServiceDomainType> getServiceDomainsFromSwitchingServices(NmlTopologyType nmlTopology, Map<String, NmlPort> portMap, NetworkType nsiNetwork, NsiTopology nsiTopology) {
        ArrayList<ServiceDomainType> serviceDomains = new ArrayList<>();
        Map<String, NmlSwitchingServiceType> newSwitchingServices = new HashMap<>();

        // The SwitchingService is modelled as a "hasService" relation.
        for (NmlTopologyRelationType relation : nmlTopology.getRelation()) {                       
            if (relation.getType().equalsIgnoreCase(Relationships.hasService)) {
                for (NmlNetworkObject service : relation.getService()) {
                    // We want the SwitchingService.
                    if (service instanceof NmlSwitchingServiceType) {
                        newSwitchingServices.put(service.getId(), (NmlSwitchingServiceType) service);
                    }
                }
            }
        }

        // Check to see if we have a SwitchingService defined, and if not,
        // create the default one with all ports and no labelSwapping.
        if (newSwitchingServices.isEmpty()) {
            NmlSwitchingServiceType switchingService = newNmlSwitchingService(nmlTopology, portMap);
            newSwitchingServices.put(switchingService.getId(), switchingService);
            
            // Now we need to add the new ServiceDefinition as a service to the
            // NSI topology model.
            for (Object object : switchingService.getAny()) {
                if (object instanceof JAXBElement) {
                    JAXBElement<?> jaxb = (JAXBElement) object;
                    if (jaxb.getValue() instanceof ServiceDefinitionType) {
                        ServiceDefinitionType serviceDefinition = (ServiceDefinitionType) jaxb.getValue();
                        ServiceType service = NsiServiceFactory.createServiceType(serviceDefinition, nsiNetwork);
                        nsiTopology.addService(service);
                        ResourceRefType serviceRef = NsiServiceFactory.createResourceRefType(service);
                        nsiNetwork.getService().add(serviceRef);
                    }
                }            
            }
        }

        // Process SwitchingService elements and created the equivalent NSI
        // Service Domain.
        for (NmlSwitchingServiceType switchingService : newSwitchingServices.values()) {
            if (switchingService.isLabelSwapping() == null ||
                    switchingService.isLabelSwapping() == Boolean.FALSE) {
                // We need a Service Domain per STP label so expand the
                // SwitchingService to hold only like labels.
                Collection<ServiceDomainType> sd = NsiServiceDomainFactory.createServiceDomains(switchingService, portMap, nsiNetwork, nsiTopology);
                serviceDomains.addAll(sd);                   
            }
            else {
                ServiceDomainType serviceDomain = NsiServiceDomainFactory.createServiceDomainType(switchingService, portMap, nsiNetwork, nsiTopology);
                serviceDomains.add(serviceDomain);
            }
        }
        
        return serviceDomains;
    }
    
    private NmlSwitchingServiceType newNmlSwitchingService(NmlTopologyType nmlTopology, Map<String, NmlPort> portMap) {
        NmlSwitchingServiceType switchingService = new NmlSwitchingServiceType();
        switchingService.setId(nmlTopology.getId() + ":SwitchingService:default");
        switchingService.setName("Default Switching Service");
        switchingService.setLabelSwapping(Boolean.FALSE);
        switchingService.setVersion(nmlTopology.getVersion());

        // Add the unidirectional port references.
        NmlSwitchingServiceRelationType hasInboundPort = new NmlSwitchingServiceRelationType();
        hasInboundPort.setType(Relationships.hasInboundPort);

        NmlSwitchingServiceRelationType hasOutboundPort = new NmlSwitchingServiceRelationType();
        hasOutboundPort.setType(Relationships.hasOutboundPort);
            
        for (NmlPort port : portMap.values()) {
            // Ignore the bidirectional ports.
            if (port.getOrientation() == Orientation.inbound) {
                NmlPortGroupType pg = new NmlPortGroupType();
                pg.setId(port.getId());
                hasInboundPort.getPortGroup().add(pg);
            }
            else if (port.getOrientation() == Orientation.outbound) {
                NmlPortGroupType pg = new NmlPortGroupType();
                pg.setId(port.getId());
                hasOutboundPort.getPortGroup().add(pg);                
            }
        }
                    
        switchingService.getRelation().add(hasInboundPort);
        switchingService.getRelation().add(hasOutboundPort);
        
        // Add a default service definition.
        ObjectFactory factory = new ObjectFactory();
        ServiceDefinitionType serviceDefinition = factory.createServiceDefinitionType();
        serviceDefinition.setId(nmlTopology.getId() + ":ServiceDefinition:default");
        serviceDefinition.setServiceType("http://services.ogf.org/nsi/2013/12/definitions/EVTS.A-GOLE");
        switchingService.getAny().add(factory.createServiceDefinition(serviceDefinition));

        return switchingService;
    }
}
