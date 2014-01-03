package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlNSAType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import org.apache.http.client.utils.DateUtils;

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
    
    public static List<NsaType> ifModifiedSince(String ifModifiedSince, List<NsaType> nsaList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<NsaType> iter = nsaList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }
        
        return nsaList;
    }
}
