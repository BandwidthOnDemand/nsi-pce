package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDefinitionType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import org.apache.http.client.utils.DateUtils;

/**
 * A factory class for generating NSI Service resource objects.
 * 
 * @author hacksaw
 */
public class NsiServiceFactory {
    private static final String NSI_ROOT_SERVICES = "/services/";

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
    
    public static List<ServiceType> ifModifiedSince(String ifModifiedSince, List<ServiceType> serviceList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<ServiceType> iter = serviceList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }
        
        return serviceList;
    }
}
