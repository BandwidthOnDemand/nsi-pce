package net.es.nsi.pce.topology.model;

import net.es.nsi.pce.topology.jaxb.NmlNSAType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;

/**
 * A factory class for generating NSI NSA resource objects.
 * @author hacksaw
 */
public class NsiNsaFactory {
    private static final String NSI_ROOT_NSAS = "/topology/nsas/";
    
    /**
     * Create a NSI NSA resource object from an NML JAXB object.
     * 
     * @param nmlNsa NML JAXB object.
     * @return
     */
    public static NsaType createNsaType(NmlNSAType nmlNsa) {
        NsaType nsiNsa = new NsaType();
        nsiNsa.setId(nmlNsa.getId());
        nsiNsa.setName(nmlNsa.getName());
        nsiNsa.setVersion(nmlNsa.getVersion());

        nsiNsa.setHref(NSI_ROOT_NSAS + nsiNsa.getId());
        
        if (nmlNsa.getLocation() != null) {
            nsiNsa.setLatitude(nmlNsa.getLocation().getLat());
            nsiNsa.setLongitude(nmlNsa.getLocation().getLong());
        }
        
        return nsiNsa;
    }
    
    /**
     * Create a resource reference for the NSI Network resource.
     * 
     * @param nsa Create a resource for this NSI NSA resource object.
     * @return 
     */
    public static ResourceRefType createResourceRefType(NsaType nsa) {
        ResourceRefType nsaRef = new ResourceRefType();
        nsaRef.setId(nsa.getId());
        nsaRef.setHref(nsa.getHref());
        return nsaRef;
    }    
}
