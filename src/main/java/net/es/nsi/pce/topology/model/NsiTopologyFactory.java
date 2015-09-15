package net.es.nsi.pce.topology.model;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.topology.jaxb.DdsDocumentListType;
import net.es.nsi.pce.topology.jaxb.DdsDocumentType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlBidirectionalPortType;
import net.es.nsi.pce.topology.jaxb.NmlLabelGroupType;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import net.es.nsi.pce.topology.jaxb.NmlNetworkObject;
import net.es.nsi.pce.topology.jaxb.NmlPortGroupRelationType;
import net.es.nsi.pce.topology.jaxb.NmlPortGroupType;
import net.es.nsi.pce.topology.jaxb.NmlPortRelationType;
import net.es.nsi.pce.topology.jaxb.NmlPortType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceRelationType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceType;
import net.es.nsi.pce.topology.jaxb.NmlTopologyRelationType;
import net.es.nsi.pce.topology.jaxb.NmlTopologyType;
import net.es.nsi.pce.topology.jaxb.NsaNsaType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ServiceDefinitionType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.provider.DdsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NsiTopologyFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PceLogger topologyLogger = PceLogger.getLogger();

    // Time we last discovered a document change.
    private long lastDiscovered = 0L;

    // The default serviceType offered by this topology.
    private String defaultServiceType = null;

    // The base URL for the REST services based on thsi topology.
    private String baseURL = null;

    /**
     * @return the lastDiscovered
     */
    public long getLastDiscovered() {
        return lastDiscovered;
    }

    /**
     * @return the defaultServiceType
     */
    public String getDefaultServiceType() {
        return defaultServiceType;
    }

    /**
     * @param defaultServiceType the defaultServiceType to set
     */
    public void setDefaultServiceType(String defaultServiceType) {
        this.defaultServiceType = defaultServiceType;
    }

    /**
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * @param baseURL the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public NsiTopology createNsiTopology(DdsDocumentListType localNsaDocuments, Map<String, DdsWrapper> nsaDocuments, DdsDocumentListType localTopologyDocuments, Map<String, DdsWrapper> topologyDocuments) throws Exception {
        log.debug("createNsiTopology: **** STARTING NML TOPOLOGY PROCESSING ****");

        // Create the NSI topology.
        NsiTopology newNsiTopology = new NsiTopology();

        // Assign the local nsaId if there is one.
        if (localNsaDocuments.getDocument().size() > 0) {
            newNsiTopology.setLocalNsaId(localNsaDocuments.getDocument().get(0).getId());
            log.debug("createNsiTopology: local nsaId=" + newNsiTopology.getLocalNsaId());
        }
        else {
            log.error("No local NSA Description document discovered so cannot assign local NSA identifier.  Pathfinding will not function!");
        }

        // Add the local network identifiers.
        List<String> networkIds = new ArrayList<>();
        for (DdsDocumentType topology : localTopologyDocuments.getDocument()) {
            log.debug("createNsiTopology: local networkId=" + topology.getId());
            networkIds.add(topology.getId());
        }
        newNsiTopology.setLocalNetworks(networkIds);

        // For each NSA document we found...
        for (DdsWrapper documentWrapper : nsaDocuments.values()) {
            log.debug("createNsiTopology: processing NSA document " + documentWrapper.getDocument().getId());
            NsaNsaType nsa = getNsaDocument(documentWrapper.getDocument());

            if (nsa == null) {
                log.debug("createNsiTopology: getNsaDocument() returned null for " + documentWrapper.getDocument().getId());
                continue;
            }

            // Create the NSI NSA resource from NSA document.
            NsaType nsiNsa = NsiNsaFactory.createNsaType(nsa, baseURL);
            nsiNsa.setDiscovered(XmlUtilities.longToXMLGregorianCalendar(documentWrapper.getDiscovered()));

            // Add it to the NSI topology.
            newNsiTopology.addNsa(nsiNsa);

            // Update the lastDiscovered time if this NSA was discovered later.
            if (documentWrapper.getDiscovered() > lastDiscovered) {
                lastDiscovered = documentWrapper.getDiscovered();
            }

            // Parse each topology document corresponding to the networks listed
            // in the NSA document.
            for (String networkId : nsa.getNetworkId()) {
                log.debug("createNsiTopology: processing networkId " + networkId);
                DdsWrapper topologyWrapper = topologyDocuments.get(networkId);
                if (topologyWrapper == null) {
                    log.debug("createNsiTopology: Could not find topology document for network " + networkId);
                    continue;
                }

                log.debug("createNsiTopology: networkId " + networkId + " maps to document " + topologyWrapper.getDocument().getId());

                NmlTopologyType nmlTopology = getTopologyDocument(topologyWrapper.getDocument());

                if (nmlTopology == null) {
                    log.debug("createNsiTopology: getTopologyDocument() returned null for " + topologyWrapper.getDocument().getId());
                    continue;
                }

                // Update the lastDiscovered time if this Topology was discovered later.
                if (topologyWrapper.getDiscovered() > lastDiscovered) {
                    lastDiscovered = topologyWrapper.getDiscovered();
                }

                // Create a new NSI Network resource.
                log.debug("createNsiTopology: Create a new NSI Network resource for " + networkId);
                NetworkType nsiNetwork = NsiNetworkFactory.createNetworkType(nmlTopology, nsiNsa);

                // Add this network resource to our NSI topology.
                newNsiTopology.addNetwork(nsiNetwork);

                // Link this Network object to the NSA object.
                nsiNsa.getNetwork().add(nsiNetwork.getSelf());

                // Convert all Service Definitions to NSI Service resources.  The
                // Service Definitions elements are held in an ANY within the NML
                // document.
                log.debug("createNsiTopology: Get service definitions for " + networkId);
                Collection<ServiceType> nsiServices;
                try {
                    nsiServices = getNsiServicesFromServiceDefinitions(nmlTopology.getAny(), nsiNetwork);
                    newNsiTopology.addAllServices(nsiServices);
                }
                catch (Exception ex) {
                    log.error("Topology contains bad service definitions: " + networkId, ex);
                    continue;
                }

                // Add the Service references to the Network resource.
                nsiServices.stream().map((service) -> {
                    log.debug("createNsiTopology: Adding service definition " + service.getId());
                    return service;
                }).forEach((service) -> {
                    nsiNetwork.getService().add(service.getSelf());
                });

                // Unidirectional ports are modelled using Relations.
                log.debug("createNsiTopology: Get unidirectional ports for " + networkId);
                Map<String, NmlPort> uniPortMap = getNmlPortsFromRelations(nmlTopology, nsiNsa);

                // Bidirectional ports are stored in separate group element and
                // reference individual undirectional Ports or PortGroups.
                Map<String, NmlPort> biPortMap = getNmlPortsFromBidirectionalPorts(nmlTopology, uniPortMap, nsiNsa);

                Map<String, NmlPort> portMap = new HashMap<>();
                portMap.putAll(uniPortMap);
                portMap.putAll(biPortMap);

                // Now we convert the unidirectional NML Ports to STP.
                log.debug("createNsiTopology: Converting unidirectional ports to STPs for " + networkId);
                Map<String, StpType> uniStp = new ConcurrentHashMap<>();
                uniPortMap.values().stream().forEach((port) -> {
                    Map<String, StpType> uniStps = NsiStpFactory.createUniStps(port, nsiNetwork);
                    uniStp.putAll(uniStps);
                    newNsiTopology.addStpBundle(port.getId(), uniStp);
                });

                // Add the port references to the Network resource.
                log.debug("createNsiTopology: Add unidirectional STP references to network resource " + networkId);
                uniStp.values().stream().forEach((stp) -> {
                    nsiNetwork.getStp().add(stp.getSelf());
                });

                log.debug("createNsiTopology: adding " + uniStp.size() + " unidirectional STP.");
                newNsiTopology.putAllStp(uniStp);

                // Now we convert the bidirectional NML Ports to STP.
                log.debug("createNsiTopology: Convert bidirectional ports to STP for " + networkId);
                Map<String, StpType> biStp = new ConcurrentHashMap<>();
                biPortMap.values().stream().forEach((port) -> {
                    Map<String, StpType> biStps = NsiStpFactory.createBiStps(port, nsiNetwork, uniStp);
                    biStp.putAll(biStps);
                    newNsiTopology.addStpBundle(port.getId(), biStps);
                });

                // Add the port references to the Network resource.
                log.debug("createNsiTopology: Add bidirectional STP references to network resource " + networkId);
                biStp.values().stream().forEach((stp) -> {
                    nsiNetwork.getStp().add(stp.getSelf());
                });

                log.debug("createNsiTopology: adding " + biStp.size() + " bidirectional STP.");
                newNsiTopology.putAllStp(biStp);

                // SwitchingServices are modelled using Relations.
                log.debug("createNsiTopology: Convert SwitchingService to NSI ServiceDomains for " + networkId);
                Collection<ServiceDomainType> nsiServiceDomains = getServiceDomainsFromSwitchingServices(nmlTopology, portMap, nsiNetwork, newNsiTopology);
                log.debug("createNsiTopology: adding " + nsiServiceDomains.size() + " Service Domains.");
                newNsiTopology.addAllServiceDomains(nsiServiceDomains);

                // Add the ServiceDomain references to the Network resource.
                log.debug("createNsiTopology: Add ServiceDomains references to " + networkId);
                nsiServiceDomains.stream().forEach((sd) -> {
                    nsiNetwork.getServiceDomain().add(sd.getSelf());
                });

                log.debug("createNsiTopology: completed processing networkId " + networkId);
            }

            log.debug("createNsiTopology: completed processing NSA document " + documentWrapper.getDocument().getId());
        }

        // We are done so update the existing topology with this new one.
        log.debug("createNsiTopology: **** COMPLETED NML TOPOLOGY PROCESSING ****");
        return newNsiTopology;
    }

    private NsaNsaType getNsaDocument(DdsDocumentType document) {
        for (Object any : document.getContent().getAny()) {
            if (any instanceof JAXBElement && ((JAXBElement) any).getValue() instanceof NsaNsaType) {
                @SuppressWarnings("unchecked")
                JAXBElement<NsaNsaType> element = (JAXBElement<NsaNsaType>) any;

                return element.getValue();
            }
        }

        return null;
    }

    private NmlTopologyType getTopologyDocument(DdsDocumentType document) {
        for (Object any : document.getContent().getAny()) {
            if (any instanceof JAXBElement && ((JAXBElement) any).getValue() instanceof NmlTopologyType) {
                @SuppressWarnings("unchecked")
                JAXBElement<NmlTopologyType> element = (JAXBElement<NmlTopologyType>) any;

                return element.getValue();
            }
        }

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

    private Map<String, NmlPort> getNmlPortsFromRelations(NmlTopologyType nmlTopology, NsaType nsa) {
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
                    // Extract the labels associated with this port.  We
                    // currently expect a single labelType with a range of
                    // values.  VLAN labels is all we currently know about.
                    Optional<String> labelType = Optional.absent();
                    Set<NmlLabelType> labels = new LinkedHashSet<>();
                    for (NmlLabelGroupType labelGroup : portGroup.getLabelGroup()) {
                        // We break out the vlan specific label.
                        labelType = Optional.fromNullable(labelGroup.getLabeltype());
                        if (NmlEthernet.isVlanLabel(labelType)) {
                            labels.addAll(NmlEthernet.labelGroupToLabels(labelGroup));
                        }
                    }

                    // PortGroup relationship has isAlias connection information.
                    String connectedTo = null;
                    int count = 0;
                    for (NmlPortGroupRelationType pgRelation : portGroup.getRelation()) {
                        if (Relationships.isAlias(pgRelation.getType())) {
                            for (NmlPortGroupType alias : pgRelation.getPortGroup()) {
                                connectedTo = alias.getId();
                                count++;
                            }
                        }
                    }

                    // Log an error if we have more than one connectedTo
                    // relationship but continue.
                    if (count > 1) {
                        topologyLogger.errorAudit(PceErrors.STP_MULTIPLE_REMOTE_REFERNCES, portGroup.getId(), Integer.toString(count));
                    }

                    // Store this port information in our scratch pad.
                    NmlPort nmlPort = new NmlPort();
                    nmlPort.setNsaId(nsa.getId());
                    nmlPort.setTopologyId(nmlTopology.getId());
                    nmlPort.setId(portGroup.getId());
                    nmlPort.setName(portGroup.getName());
                    nmlPort.setEncoding(Optional.fromNullable(portGroup.getEncoding()));
                    nmlPort.setLabelType(labelType);
                    nmlPort.setOrientation(orientation);
                    nmlPort.setVersion(nsa.getVersion());
                    nmlPort.setDiscovered(getLastDiscovered());
                    nmlPort.setLabels(labels);
                    nmlPort.setConnectedTo(connectedTo);
                    portMap.put(nmlPort.getId(), nmlPort);
                }

                // Some topologies use Port to model unidirectional ports.
                for (NmlPortType port : relation.getPort()) {
                    // PortGroup relationship has isAlias connection information.
                    String connectedTo = null;
                    int count = 0;
                    for (NmlPortRelationType pgRelation : port.getRelation()) {
                        if (Relationships.isAlias(pgRelation.getType())) {
                            for (NmlPortType alias : pgRelation.getPort()) {
                                connectedTo = alias.getId();
                                count++;
                            }
                        }
                    }

                    // Log an error if we have more than one connectedTo
                    // relationship but continue.
                    if (count > 1) {
                        topologyLogger.errorAudit(PceErrors.STP_MULTIPLE_REMOTE_REFERNCES, port.getId(), Integer.toString(count));
                    }

                    // Store this port information in our scratch pad.
                    NmlPort nmlPort = new NmlPort();
                    nmlPort.setNsaId(nsa.getId());
                    nmlPort.setTopologyId(nmlTopology.getId());
                    nmlPort.setId(port.getId());
                    nmlPort.setName(port.getName());
                    nmlPort.setEncoding(Optional.fromNullable(port.getEncoding()));
                    nmlPort.setOrientation(orientation);
                    nmlPort.setVersion(nsa.getVersion());
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

    private Map<String, NmlPort> getNmlPortsFromBidirectionalPorts(NmlTopologyType nmlTopology, Map<String, NmlPort> uniPortMap, NsaType nsa) {
        Map<String, NmlPort> biPortMap = new HashMap<>();

        // Bidirectional ports are stored in separate group element and
        // reference individual undirection Ports or PortGroups.
        List<NmlNetworkObject> groups = nmlTopology.getGroup();
        for (NmlNetworkObject group : groups) {
            // Process the BidirectionalPort.  It will contain references
            // to either member Port or PortGroup elements.
            if (group instanceof NmlBidirectionalPortType) {
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
                            NmlPort tmp = uniPortMap.get(pg.getId());
                            if (tmp != null) {
                                if (tmp.getOrientation() == Orientation.inbound) {
                                    inbound = tmp;
                                }
                                else if (tmp.getOrientation() == Orientation.outbound) {
                                    outbound = tmp;
                                }
                                else {
                                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_INVALID_MEMEBER_VALUE, port.getId(), "has invalid undirectional port group reference (" + pg.getId() + ") orientation type (" + tmp.getOrientation().toString() + ").");
                                }
                            }
                        }
                        else if (element.getValue() instanceof NmlPortType) {
                            NmlPortType p = (NmlPortType) element.getValue();
                            NmlPort tmp = uniPortMap.get(p.getId());
                            if (tmp != null) {
                                if (tmp.getOrientation() == Orientation.inbound) {
                                    inbound = tmp;
                                }
                                else if (tmp.getOrientation() == Orientation.outbound) {
                                    outbound = tmp;
                                }
                                else {
                                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_INVALID_MEMEBER_VALUE, port.getId(), "has invalid undirectional port group reference (" + p.getId() + ") orientation type (" + tmp.getOrientation().toString() + ").");

                                }
                            }
                        }
                    }
                }

                if (inbound == null) {
                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_MISSING_INBOUND_STP, port.getId());
                    continue;
                }
                else if (outbound == null) {
                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_MISSING_OUTBOUND_STP, port.getId());
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
                            topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_LABEL_RANGE_MISMATCH, port.getId(), inLabel.getLabeltype() + "=" + inLabel.getValue());
                            consistent = false;
                            break;
                        }
                    }

                    if (!consistent) {
                      continue;
                    }
                }
                else if (inLabels == null){
                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_LABEL_RANGE_MISMATCH, port.getId(), "no inbound STP labels");
                    continue;
                }
                else if (outLabels == null) {
                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_LABEL_RANGE_MISMATCH, port.getId(), "no outbound STP labels");
                    continue;
                }

                // Build the bidirection Port using unidirectional Labels
                // and store this port information in our scratch pad.
                NmlPort nmlPort = new NmlPort();
                nmlPort.setNsaId(nsa.getId());
                nmlPort.setTopologyId(nmlTopology.getId());
                nmlPort.setId(port.getId());
                nmlPort.setName(port.getName());
                nmlPort.setEncoding(inbound.getEncoding());
                nmlPort.setOrientation(Orientation.inboundOutbound);
                nmlPort.setVersion(nsa.getVersion());
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

        // NML Default Behavior #1: Check to see if we have a SwitchingService
        // defined, and if not, create the default one with all ports and
        // labelSwapping set to false.
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
                        nsiNetwork.getService().add(service.getSelf());
                    }
                }
            }
        }
        else {
            // NML Default Behavior #2: If we have a SwitchingService with no port
            // members then we must add all ports of matching label and encoding
            // type.  We use a boolean here to tell us if the SwitchingService
            // held a port.
            for (NmlSwitchingServiceType switchingService : newSwitchingServices.values()) {
                boolean foundPort = false;
                for (NmlSwitchingServiceRelationType relation : switchingService.getRelation()) {
                    if (Relationships.hasInboundPort.equalsIgnoreCase(relation.getType())) {
                        foundPort = true;
                        break;
                    }
                    else if (Relationships.hasOutboundPort.equalsIgnoreCase(relation.getType())) {
                        foundPort = true;
                        break;
                    }
                }

                if (!foundPort) {
                    // Treat this as a wildcard SwitchingService buy adding all
                    // unidirectional ports with maching attributes.
                    populateWildcardSwitchingService(switchingService, portMap);
                }
            }
        }

        // Process SwitchingService elements and create the equivalent NSI
        // Service Domain.
        for (NmlSwitchingServiceType switchingService : newSwitchingServices.values()) {
            if (Objects.equals(switchingService.isLabelSwapping(), Boolean.TRUE)) {
                log.debug("Processing SwitchingService with labelSwapping == true, id=" + switchingService.getId());
                ServiceDomainType serviceDomain = NsiServiceDomainFactory.createServiceDomainType(switchingService, portMap, nsiNetwork, nsiTopology);
                serviceDomains.add(serviceDomain);
            }
            else {
                // We need a Service Domain per STP label so expand the
                // SwitchingService to hold only like labels.
                log.debug("Processing SwitchingService with labelSwapping == false, id=" + switchingService.getId());
                Collection<ServiceDomainType> sd = NsiServiceDomainFactory.createServiceDomains(switchingService, portMap, nsiNetwork, nsiTopology);
                serviceDomains.addAll(sd);
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
        serviceDefinition.setServiceType(defaultServiceType);
        switchingService.getAny().add(factory.createServiceDefinition(serviceDefinition));

        return switchingService;
    }

    private NmlSwitchingServiceType populateWildcardSwitchingService(NmlSwitchingServiceType switchingService, Map<String, NmlPort> portMap) {
        log.debug("Found empty SwitchingService, id=" + switchingService.getId() + ", populating based on wildcard rules.");
        Optional<String> encoding = Optional.fromNullable(switchingService.getEncoding());
        Optional<String> labelType = Optional.fromNullable(switchingService.getLabelType());

        log.debug("encoding = " + encoding);
        log.debug("labelType = " + labelType);

        // Add the unidirectional port references.
        NmlSwitchingServiceRelationType hasInboundPort = new NmlSwitchingServiceRelationType();
        hasInboundPort.setType(Relationships.hasInboundPort);

        NmlSwitchingServiceRelationType hasOutboundPort = new NmlSwitchingServiceRelationType();
        hasOutboundPort.setType(Relationships.hasOutboundPort);

        for (NmlPort port : portMap.values()) {
            if (port.getEncoding().equals(encoding) && port.getLabelType().equals(labelType)) {
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
        }

        switchingService.getRelation().add(hasInboundPort);
        switchingService.getRelation().add(hasOutboundPort);

        return switchingService;
    }
}
