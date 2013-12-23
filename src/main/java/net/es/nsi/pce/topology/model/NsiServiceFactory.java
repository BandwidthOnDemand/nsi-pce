package net.es.nsi.pce.topology.model;

import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDefinitionType;
import net.es.nsi.pce.topology.jaxb.ServiceType;

/**
 * A factory class for generating NSI Service resource objects.
 * 
 * @author hacksaw
 */
public class NsiServiceFactory {
    private static final String NSI_ROOT_SERVICES = "/topology/services/";

    /**
     * Create a NSI Service resource object from an NML JAXB object.
     * 
     * @param serviceDefinition NML ServiceDefinition JAXB object.
     * @param nsiNetwork The NSI Network resource object used for creating Network references as well as setting discovered and version information.
     * @return 
     */
    public static ServiceType createServiceType(ServiceDefinitionType serviceDefinition, NetworkType nsiNetwork) {
        ServiceType nsiService = new ServiceType();
        nsiService.setId(serviceDefinition.getId());
        String name = serviceDefinition.getName();
        if (name == null || name.isEmpty()) {
            name = serviceDefinition.getId();
        }
        nsiService.setName(name);
        nsiService.setType(serviceDefinition.getServiceType());
        nsiService.setDiscovered(nsiNetwork.getDiscovered());
        nsiService.setVersion(nsiNetwork.getVersion());
        nsiService.setHref(NSI_ROOT_SERVICES + serviceDefinition.getId());

        ResourceRefType nsiNetworkRef = NsiNetworkFactory.createResourceRefType(nsiNetwork);
        nsiService.setNetwork(nsiNetworkRef);

        nsiService.setServiceDefinition(serviceDefinition);              
        
        return nsiService;
    }

    /**
     * Create a resource reference for the NSI Service resource.
     * 
     * @param service Create a resource for this NSI Service resource object.
     * @return 
     */
    public static ResourceRefType createResourceRefType(ServiceType service) {
        ResourceRefType serviceRef = new ResourceRefType();
        serviceRef.setId(service.getId());
        serviceRef.setHref(service.getHref());
        serviceRef.setType(service.getType());
        return serviceRef;
    }    
}
