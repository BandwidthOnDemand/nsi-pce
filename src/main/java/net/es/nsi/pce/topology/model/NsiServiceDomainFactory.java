package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlPortGroupType;
import net.es.nsi.pce.topology.jaxb.NmlPortType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceRelationType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDefinitionType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.provider.TopologyParser;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory class for generating NSI ServiceDomain resource objects.
 *
 * @author hacksaw
 */
public class NsiServiceDomainFactory {

    private static final String NSI_ROOT_SERVICEDOMAINS = "/serviceDomains/";
    private static final PceLogger topologyLogger = PceLogger.getLogger();

    /**
     * Create a NSI ServiceDomain resource object from an NML JAXB object.
     *
     * @param switchingService The NML SwitchingService JAXB object that defines the NSI ServiceDomain.
     * @param portMap The map of NML ports within this network.
     * @param nsiNetwork The NSI Network resource containing this ServiceDomain.
     * @param nsiTopology The NSI Topology resource object used for looking up service definitions and STPs.
     *
     * @return New ServiceDomain resource object.
     */
    public static ServiceDomainType createServiceDomainType(NmlSwitchingServiceType switchingService, Map<String, NmlPort> portMap, NetworkType nsiNetwork, NsiTopology nsiTopology) {
        Logger log = LoggerFactory.getLogger(NsiServiceDomainFactory.class);

        ServiceDomainType nsiServiceDomain = new ServiceDomainType();

        // We map the SwitchingService Id to the Service Domain Id.
        nsiServiceDomain.setId(switchingService.getId());

        // Use the SwitchingService name if provided otherwise the Id.
        String name = switchingService.getName();
        if (name == null || name.isEmpty()) {
            name = switchingService.getId();
        }
        nsiServiceDomain.setName(name);

        // Use the discovered and version info from our Network resource.
        nsiServiceDomain.setDiscovered(nsiNetwork.getDiscovered());
        nsiServiceDomain.setVersion(nsiNetwork.getVersion());

        // Href is mapped using the ServiceDomain Id.
        nsiServiceDomain.setHref(NSI_ROOT_SERVICEDOMAINS + nsiServiceDomain.getId());

        // We need references to the containing Network.
        ResourceRefType nsiNetworkRef = NsiNetworkFactory.createResourceRefType(nsiNetwork);
        nsiServiceDomain.setNetwork(nsiNetworkRef);

        // Map throuhg the label swapping attribute.
        if (switchingService.isLabelSwapping() == null ||
                    switchingService.isLabelSwapping() == Boolean.FALSE) {
            nsiServiceDomain.setLabelSwapping(false);
        }
        else {
            nsiServiceDomain.setLabelSwapping(true);
        }

        // We will have one and only one associated Service Definition but it
        // is carried in an ANY.  Pull out the first one we find and map to
        // an NSI Service resource.
        ServiceType service = null;
        for (Object object : switchingService.getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> jaxb = (JAXBElement) object;
                if (jaxb.getValue() instanceof ServiceDefinitionType) {
                    ServiceDefinitionType serviceDefinition = (ServiceDefinitionType) jaxb.getValue();
                    service = nsiTopology.getService(serviceDefinition.getId());
                    break;
                }
            }
        }

        // Make sure we found a corresponding service before storing.
        if (service != null) {
            ResourceRefType nsiServiceRef = NsiServiceFactory.createResourceRefType(service);
            nsiServiceDomain.setService(nsiServiceRef);
        }
        else {
            log.error("NML SwitchingService " + switchingService.getId() + " does not have a valid ServiceDefinition.");
            topologyLogger.errorAudit(PceErrors.SERVICE_DOMAIN_SERVICEDEFINITION, switchingService.getId());
        }

        // Now we add the port references that are stored in Relations.
        for (NmlSwitchingServiceRelationType relation : switchingService.getRelation()) {
            if (!Relationships.hasInboundPort.equals(relation.getType()) &&
                    !Relationships.hasOutboundPort.equals(relation.getType())) {
                log.error("NML SwitchingService " + switchingService.getId() + " has an unsupported relationship " + relation.getType());
                topologyLogger.errorAudit(PceErrors.SERVICE_DOMAIN_UNSUPPORTED_RELATIONSHIP, switchingService.getId(), relation.getType());
                continue;
            }

            // First we will do the port relations.
            for (NmlPortType port : relation.getPort()) {
                NmlPort nmlPort = portMap.get(port.getId());
                if (nmlPort == null) {
                    log.error("NML SwitchingService " + switchingService.getId() + " has an undefined unidirectional port " + port.getId());
                    topologyLogger.errorAudit(PceErrors.SERVICE_DOMAIN_UNDEFINED_STP, switchingService.getId(), port.getId());
                    continue;
                }

                mapPortToStp(nmlPort, relation.getType(), nsiServiceDomain, nsiTopology);
            }

            // Now the PortGroup relations.
            for (NmlPortGroupType portGroup : relation.getPortGroup()) {
                NmlPort nmlPort = portMap.get(portGroup.getId());
                if (nmlPort == null) {
                    log.error("NML SwitchingService " + switchingService.getId() + " has an undefined unidirectional port group " + portGroup.getId());
                    topologyLogger.errorAudit(PceErrors.SERVICE_DOMAIN_UNDEFINED_STP, switchingService.getId(), portGroup.getId());
                    continue;
                }

                mapPortToStp(nmlPort, relation.getType(), nsiServiceDomain, nsiTopology);
            }
        }

        // Create a ServiceDomain reference for populating STP.
        ResourceRefType serviceDomainRef = NsiServiceDomainFactory.createResourceRefType(nsiServiceDomain);

        // Now we add all the bidirectional ports that have unidirectional
        // members of this SwitchingService.  This is slow...
        for (ResourceRefType inboundStp : nsiServiceDomain.getInboundStp()) {
            for (StpType stp : nsiTopology.getStps()) {
                if (StpDirectionalityType.BIDIRECTIONAL == stp.getType()) {
                    if (stp.getInboundStp().getId().equalsIgnoreCase(inboundStp.getId())) {
                        // We should check for a matching outbound STP but skip for now.
                        ResourceRefType stpRef = NsiStpFactory.createResourceRefType(stp);
                        nsiServiceDomain.getBidirectionalStp().add(stpRef);
                        stp.setServiceDomain(serviceDomainRef);
                    }
                }
            }
        }

        // Add this ServiceDomain to the owning network.
        nsiNetwork.getServiceDomain().add(serviceDomainRef);
        return nsiServiceDomain;
    }

    /**
     * Create a resource reference for the NSI ServiceDomain resource.
     *
     * @param serviceDomain Create a resource for this NSI ServiceDomain resource object.
     *
     * @return The new resource reference.
     */
    public static ResourceRefType createResourceRefType(ServiceDomainType serviceDomain) {
        ResourceRefType serviceDomainRef = new ResourceRefType();
        serviceDomainRef.setId(serviceDomain.getId());
        serviceDomainRef.setHref(serviceDomain.getHref());
        serviceDomainRef.setType(serviceDomain.getService().getType());
        return serviceDomainRef;
    }

    /**
     * This method maps an NML Port to the matching STPs stored in the nsiTopology
     * object, and then adds the associated STP references to the provided
     * Service Domain.
     *
     * @param nmlPort The NML Port to be mapped through to a list of STP.
     * @param relationType Defines whether this is an inbound or outbound port.
     * @param nsiServiceDomain The Service Domain to be populated with matching STP.
     * @param nsiTopology NSI topology containing STP resource objects.
     */
    private static void mapPortToStp(NmlPort nmlPort, String relationType, ServiceDomainType nsiServiceDomain, NsiTopology nsiTopology) {
        // Create a ServiceDomain reference for populating STP.
        ResourceRefType serviceDomainRef = NsiServiceDomainFactory.createResourceRefType(nsiServiceDomain);

        // Retrieve the STP so we can build a reference.
        StpType stp = nsiTopology.getStp(nmlPort.getId());
        stp.setServiceDomain(serviceDomainRef);
        ResourceRefType stpRef = NsiStpFactory.createResourceRefType(stp);
        switch (relationType) {
            case Relationships.hasInboundPort:
                nsiServiceDomain.getInboundStp().add(stpRef);
                break;
            case Relationships.hasOutboundPort:
                nsiServiceDomain.getOutboundStp().add(stpRef);
                break;
        }
    }

    public static List<ServiceDomainType> ifModifiedSince(String ifModifiedSince, List<ServiceDomainType> serviceDomainList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<ServiceDomainType> iter = serviceDomainList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }

        return serviceDomainList;
    }
}
