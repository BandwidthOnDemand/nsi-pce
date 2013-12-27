package net.es.nsi.pce.topology.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.config.topo.nml.Relationships;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import net.es.nsi.pce.topology.jaxb.NmlPortGroupType;
import net.es.nsi.pce.topology.jaxb.NmlPortType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceRelationType;
import net.es.nsi.pce.topology.jaxb.NmlSwitchingServiceType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDefinitionType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.provider.NmlPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory class for generating NSI ServiceDomain resource objects.
 * 
 * @author hacksaw
 */
public class NsiServiceDomainFactory {
    
    private static final String NSI_ROOT_SERVICEDOMAINS = "/topology/serviceDomains/";

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
        }
        
        // Now we add the port references that are stored in Relations.
        for (NmlSwitchingServiceRelationType relation : switchingService.getRelation()) {
            if (!Relationships.hasInboundPort.equals(relation.getType()) &&
                    !Relationships.hasOutboundPort.equals(relation.getType())) {
                log.error("NML SwitchingService " + switchingService.getId() + " has an unsupported relationship " + relation.getType()); 
                continue;
            }
            
            // First we will do the port relations.
            for (NmlPortType port : relation.getPort()) {
                NmlPort nmlPort = portMap.get(port.getId());
                if (nmlPort == null) {
                    log.error("NML SwitchingService " + switchingService.getId() + " has an undefined unidirectional port " + port.getId());
                    continue;
                }
                
                mapPortToStp(nmlPort, relation.getType(), nsiServiceDomain, nsiTopology);
            }
            
            // Now the PortGroup relations.
            List<NmlPortGroupType> portGroups = relation.getPortGroup();
            for (NmlPortGroupType portGroup : relation.getPortGroup()) {
                NmlPort nmlPort = portMap.get(portGroup.getId());
                if (nmlPort == null) {
                    log.error("NML SwitchingService " + switchingService.getId() + " has an undefined unidirectional port group " + portGroup.getId());
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
        
        // If there are no labels associated with the port then we have a single STP.
        if (nmlPort.getLabels() == null || nmlPort.getLabels().isEmpty()) {
            String stpId = NsiStpFactory.createStpId(nmlPort.getId(), null);

            // Retrieve the STP so re can build a reference.
            StpType stp = nsiTopology.getStp(stpId);
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
        else {
            // Generate the STP identifiers associated with each port and label.
            for (NmlLabelType label : nmlPort.getLabels()) {
                String stpId = NsiStpFactory.createStpId(nmlPort.getId(), label);

                // Retrieve the STP so re can build a reference.
                StpType stp = nsiTopology.getStp(stpId);
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
        }        
    }

    /**
     * 
     * @param portId
     * @param portMap
     * @param nsiTopology
     * @param results 
     */
    private static void getStpByPortId(String portId, Map<String, NmlPort> portMap, NsiTopology nsiTopology, Map<String, ArrayList<StpType>> results) {

        NmlPort nmlPort = portMap.get(portId);
        if (nmlPort == null) {
            throw new IllegalArgumentException("Undefined unidirectional port " + portId);
        }

        // A port with no label is a special condition.
        Set<NmlLabelType> labels = nmlPort.getLabels();
        if (labels == null || labels.isEmpty()) {
            // Map port to STP.
            String stpId = NsiStpFactory.createStpId(portId, null);
            StpType nsiStp = nsiTopology.getStp(stpId);

            // See if we have already started a map with no labels.
            ArrayList<StpType> stps = results.get("NOLABEL");
            if (stps == null) {
                stps = new ArrayList<>();
            }

            stps.add(nsiStp);
            results.put("NOLABEL", stps);
        }
        else {
            for (NmlLabelType label : labels) {
                // Map port to STP.
                String stpId = NsiStpFactory.createStpId(portId, label);
                StpType nsiStp = nsiTopology.getStp(stpId);

                // See if we have already started a map with this label.
                String key = label.getLabeltype() + "=" + label.getValue();
                ArrayList<StpType> stps = results.get(key);
                if (stps == null) {
                    stps = new ArrayList<>();
                }

                stps.add(nsiStp);
                results.put(key, stps);
            }
        }
    }
    
    /**
     * 
     * @param id
     * @param portList
     * @param portMap
     * @param nsiTopology
     * @param results 
     */
    private static void extractPortsToStp(String id, List<NmlPortType> portList, Map<String, NmlPort> portMap, NsiTopology nsiTopology, Map<String, ArrayList<StpType>> results) {
        Logger log = LoggerFactory.getLogger(NsiServiceDomainFactory.class);
        
        for (NmlPortType port : portList) {
            try {
                getStpByPortId(port.getId(), portMap, nsiTopology, results);
            } catch (IllegalArgumentException ex) {
                log.error("NML SwitchingService " + id + " has an undefined unidirectional port " + port.getId(), ex);
                continue;
            }
        }
    }
    
    /**
     * 
     * @param id
     * @param portGroupList
     * @param portMap
     * @param nsiTopology
     * @param results 
     */
    private static void extractPortGroupsToStp(String id, List<NmlPortGroupType> portGroupList, Map<String, NmlPort> portMap, NsiTopology nsiTopology, Map<String, ArrayList<StpType>> results) {
        Logger log = LoggerFactory.getLogger(NsiServiceDomainFactory.class);
        
        for (NmlPortGroupType port : portGroupList) {
            try {
                getStpByPortId(port.getId(), portMap, nsiTopology, results);
            } catch (IllegalArgumentException ex) {
                log.error("NML SwitchingService " + id + " has an undefined unidirectional port " + port.getId(), ex);
                continue;
            }
        }
    }
    
    /**
     * 
     * @param switchingService
     * @param portMap
     * @param nsiNetwork
     * @param nsiTopology
     * @return 
     */
    public static Collection<ServiceDomainType> createServiceDomains(NmlSwitchingServiceType switchingService, Map<String, NmlPort> portMap, NetworkType nsiNetwork, NsiTopology nsiTopology) {
        Logger log = LoggerFactory.getLogger(NsiServiceDomainFactory.class);
        Collection<ServiceDomainType> results = new ArrayList<>();
        
        // If we do not need to expand then return existing SwitchingService.
        if (switchingService.isLabelSwapping() != null &&
                    switchingService.isLabelSwapping() == Boolean.TRUE) {
            results.add(createServiceDomainType(switchingService, portMap, nsiNetwork, nsiTopology));
            return results;
        }
        
        // Parse out ports and port groups.  Map to STP and sort based on label
        // value.
        HashMap<String, ArrayList<StpType>> inboundStps = new HashMap<>();
        HashMap<String, ArrayList<StpType>> outboundStps = new HashMap<>();
        for (NmlSwitchingServiceRelationType relation : switchingService.getRelation()) {
            if (Relationships.hasInboundPort.equalsIgnoreCase(relation.getType())) {
                // First we will do the port relations.
                extractPortsToStp(switchingService.getId(), relation.getPort(), portMap, nsiTopology, inboundStps);
                extractPortGroupsToStp(switchingService.getId(), relation.getPortGroup(), portMap, nsiTopology, inboundStps);
            }
            else if (Relationships.hasOutboundPort.equalsIgnoreCase(relation.getType())) {
                extractPortsToStp(switchingService.getId(), relation.getPort(), portMap, nsiTopology, outboundStps);
                extractPortGroupsToStp(switchingService.getId(), relation.getPortGroup(), portMap, nsiTopology, outboundStps);
            }
        }
        
        // We need references to the containing Network.
        ResourceRefType nsiNetworkRef = NsiNetworkFactory.createResourceRefType(nsiNetwork);

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
        ResourceRefType nsiServiceRef = null;
        if (service != null) {
            nsiServiceRef = NsiServiceFactory.createResourceRefType(service);
        }
        else {
            log.error("NML SwitchingService " + switchingService.getId() + " does not have a valid ServiceDefinition.");
        }
        
        // Make a copy of the Bidirectional STP and remove them as we
        // populate their member unidirectional ports.
        Map<String, StpType> biStp = new HashMap<>();
        for (StpType stp : nsiTopology.getStps()) {
            if (stp.getType() == StpDirectionalityType.BIDIRECTIONAL) {
                biStp.put(stp.getInboundStp().getId(), stp);
                biStp.put(stp.getOutboundStp().getId(), stp);
            }
        }
        
        // Now for each key we create a new ServiceDomain.
        for (String key : inboundStps.keySet()) {
            ServiceDomainType nsiServiceDomain = new ServiceDomainType();
            
            // Build a unique identifier for this SwitchingService.
            StringBuilder identifier = new StringBuilder(switchingService.getId());
            identifier.append("?");
            int pos = key.lastIndexOf("#");
            String type = key.substring(pos+1);
            if (type != null && !type.isEmpty()) {
                identifier.append(type);
            }
            else {
                identifier.append(key);
            }
            
            // We map the SwitchingService Id to the Service Domain Id.
            nsiServiceDomain.setId(identifier.toString());
        
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
            nsiServiceDomain.setNetwork(nsiNetworkRef);

            // Make sure we found a corresponding service before storing.
            nsiServiceDomain.setService(nsiServiceRef);   
            
            // Make a reference for this ServiceDomain and add to each member STP.
            ResourceRefType nsiServiceDomainRef = NsiServiceDomainFactory.createResourceRefType(nsiServiceDomain);
            

            // Add the inbound STP references to this ServiceDomain.
            List<StpType> inStps = inboundStps.get(key);
            for (StpType inStp : inStps) {
                nsiServiceDomain.getInboundStp().add(NsiStpFactory.createResourceRefType(inStp));
                inStp.setServiceDomain(nsiServiceDomainRef);
                
                // Check to see if there is a corresponding bidirectional STP.
                StpType inBiStp = biStp.remove(inStp.getId());
                if (inBiStp != null) {
                    nsiServiceDomain.getBidirectionalStp().add(NsiStpFactory.createResourceRefType(inBiStp));
                    inBiStp.setServiceDomain(nsiServiceDomainRef);
                    
                    // Remove the outbound entry for this bidirectional STP.
                    biStp.remove(inBiStp.getOutboundStp().getId());
                }
            }
            
            // Add the outbound STP references to this ServiceDomain.
            List<StpType> outStps = outboundStps.get(key);
            for (StpType outStp : outStps) {
                nsiServiceDomain.getOutboundStp().add(NsiStpFactory.createResourceRefType(outStp));
                outStp.setServiceDomain(nsiServiceDomainRef);
                
                // Inbound unidirectional STP members would have already removed 
                // their corresponding bidrectional STP, however, there maybe
                // a mismatch that needs to be handled.
                StpType outBiStp = biStp.remove(outStp.getId());
                if (outBiStp != null) {
                    nsiServiceDomain.getBidirectionalStp().add(NsiStpFactory.createResourceRefType(outBiStp));
                    outBiStp.setServiceDomain(nsiServiceDomainRef);
                }                
            }

            results.add(nsiServiceDomain);
        }
        
        return results;
    }
}
