package net.es.nsi.pce.topology.model;

import net.es.nsi.pce.management.logs.PceErrors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlBidirectionalPortType;
import net.es.nsi.pce.topology.jaxb.NmlLabelGroupType;
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
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.topology.jaxb.DdsDocumentListType;
import net.es.nsi.pce.topology.jaxb.DdsDocumentType;
import net.es.nsi.pce.topology.jaxb.NsaNsaType;
import net.es.nsi.pce.topology.jaxb.TypeValueType;
import net.es.nsi.pce.topology.provider.DdsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NsiTopologyFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private PceLogger topologyLogger = PceLogger.getLogger();

    // Time we last discovered a document change.
    private long lastDiscovered = 0L;

    // The default serviceType offered by this topology.
    private String defaultServiceType = null;
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

    public NsiTopology createNsiTopology(DdsDocumentListType localNsaDocuments,
            Map<String, DdsWrapper> nsaDocuments,
            DdsDocumentListType localTopologyDocuments,
            Map<String, DdsWrapper> topologyDocuments) throws Exception {
        // Create the NSI topology.
        NsiTopology newNsiTopology = new NsiTopology();

        // Assign the local nsaId if there is one.
        if (localNsaDocuments.getDocument().size() > 0) {
            newNsiTopology.setLocalNsaId(localNsaDocuments.getDocument().get(0).getId());
            log.debug("createNsiTopology: local nsaId=" + newNsiTopology.getLocalNsaId());
        }

        List<String> networkIds = new ArrayList<>();
        for (DdsDocumentType topology : localTopologyDocuments.getDocument()) {
            log.debug("createNsiTopology: local networkId=" + topology.getId());
            networkIds.add(topology.getId());
        }
        newNsiTopology.setLocalNetworks(networkIds);

        // For each NSA document we found...
        for (DdsWrapper documentWrapper : nsaDocuments.values()) {
            log.debug("createNsiTopology: processing NSA " + documentWrapper.getDocument().getId());
            NsaNsaType nsa = getNsaDocument(documentWrapper.getDocument());

            if (nsa == null) {
                log.debug("createNsiTopology: getNsaDocument() returned null for " + documentWrapper.getDocument().getId());
                continue;
            }

            // Create the NSI NSA resource from NSA document.
            NsaType nsiNsa = NsiNsaFactory.createNsaType(nsa);
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
                DdsWrapper topologyWrapper = topologyDocuments.get(networkId);
                if (topologyWrapper == null) {
                    log.debug("createNsiTopology: Could not find topology document for network " + networkId);
                    continue;
                }

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
                Map<String, NmlPort> uniPortMap = getNmlPortsFromRelations(nmlTopology, nsiNsa);

                // Bidirectional ports are stored in separate group element and
                // reference individual undirection Ports or PortGroups.
                Map<String, NmlPort> biPortMap = getNmlPortsFromBidirectionalPorts(nmlTopology, uniPortMap, nsiNsa);

                Map<String, NmlPort> portMap = new HashMap<>();
                portMap.putAll(uniPortMap);
                portMap.putAll(biPortMap);

                // Now we convert the unidirectional NML Ports to STP.
                for (NmlPort port : uniPortMap.values()) {
                    StpType newStp = NsiStpFactory.createStpType(port, newNsiTopology);
                    newNsiTopology.addStp(newStp);

                    // Add the port references to the Network resource.
                    ResourceRefType stpRef = NsiStpFactory.createResourceRefType(newStp);
                    nsiNetwork.getStp().add(stpRef);
                }

                // Now we convert the bidirectional NML Ports to STP.
                for (NmlPort port : biPortMap.values()) {
                    StpType newStp = NsiStpFactory.createStpType(port, newNsiTopology);
                    newNsiTopology.addStp(newStp);

                    // Add the port references to the Network resource.
                    ResourceRefType stpRef = NsiStpFactory.createResourceRefType(newStp);
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
        }

        // We are done so update the existing topology with this new one.
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
                    List<TypeValueType> labels = new ArrayList<>();
                    for (NmlLabelGroupType labelGroup : portGroup.getLabelGroup()) {
                        try {
                            labels.add(LabelUtilities.nmlLabelGroupToLabel(labelGroup));
                        }
                        catch (IllegalArgumentException ex) {
                            topologyLogger.errorAudit(PceErrors.STP_UNKNOWN_LABEL_TYPE, portGroup.getId(), ex.getMessage());
                        }
                    }

                    labels = LabelUtilities.sortLabels(labels);

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
                    nmlPort.setOrientation(orientation);
                    nmlPort.setVersion(nsa.getVersion());
                    nmlPort.setDiscovered(getLastDiscovered());
                    List<TypeValueType> labels = new ArrayList<>();
                    try {
                        labels.add(LabelUtilities.nmlLabelToLabel(port.getLabel()));
                    }
                    catch (IllegalArgumentException ex) {
                        topologyLogger.errorAudit(PceErrors.STP_UNKNOWN_LABEL_TYPE, port.getId(), ex.getMessage());
                    }
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

                // We need to have identical label sets or we reject the STP.
                List<TypeValueType> inLabels = inbound.getLabels();
                List<TypeValueType> outLabels = outbound.getLabels();

                if (!LabelUtilities.equals(inLabels, outLabels)) {
                    // We have a missmatch on label values for component STP so don't create a Bidirectional STP.
                    topologyLogger.errorAudit(PceErrors.BIDIRECTIONAL_STP_LABEL_RANGE_MISMATCH, port.getId(), "inbound and outbound STP labels do not match.");
                    continue;
                }

                // Build the bidirection Port using unidirectional Labels
                // and store this port information in our scratch pad.
                NmlPort nmlPort = new NmlPort();
                nmlPort.setNsaId(nsa.getId());
                nmlPort.setTopologyId(nmlTopology.getId());
                nmlPort.setId(port.getId());
                nmlPort.setName(port.getName());
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

    private Collection<ServiceDomainType> getServiceDomainsFromSwitchingServices(
            NmlTopologyType nmlTopology, Map<String, NmlPort> portMap,
            NetworkType nsiNetwork, NsiTopology nsiTopology) {
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
            ServiceDomainType serviceDomain = NsiServiceDomainFactory.createServiceDomainType(switchingService, portMap, nsiNetwork, nsiTopology);
            serviceDomains.add(serviceDomain);
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
}
