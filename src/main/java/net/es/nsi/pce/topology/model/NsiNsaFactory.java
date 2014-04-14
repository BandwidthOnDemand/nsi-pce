package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.jaxb.NsaHolderType;
import net.es.nsi.pce.topology.jaxb.NsaNsaType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import org.apache.http.client.utils.DateUtils;

/**
 * A factory class for generating NSI NSA resource objects.
 * @author hacksaw
 */
public class NsiNsaFactory {
    private static final String NSI_ROOT_NSAS = "/nsas/";
    
    /**
     * Create a NSI NSA resource object from an NML JAXB object.
     * 
     * @param nmlNsa NML JAXB object.
     * @return
     */
    public static NsaType createNsaType(NsaNsaType nsa) {
        NsaType nsiNsa = new NsaType();
        nsiNsa.setId(nsa.getId());
        nsiNsa.setName(nsa.getName());
        nsiNsa.setVersion(nsa.getVersion());
        nsiNsa.setHref(NSI_ROOT_NSAS + nsiNsa.getId());

        nsiNsa.setSoftwareVersion(nsa.getSoftwareVersion());
        nsiNsa.setStartTime(nsa.getStartTime());
        nsiNsa.setLocation(nsa.getLocation());
        nsiNsa.setAdminContact(nsa.getAdminContact());
        
        if (nsa.getInterface() != null && !nsa.getInterface().isEmpty()) {
            nsiNsa.getInterface().addAll(nsa.getInterface());
        }
        
        if (nsa.getFeature() != null && !nsa.getFeature().isEmpty()) {
            nsiNsa.getFeature().addAll(nsa.getFeature());
        }
        
        if (nsa.getPeersWith() != null && !nsa.getPeersWith().isEmpty()) {
            nsiNsa.getPeersWith().addAll(nsa.getPeersWith());
        }
        
        for (NsaHolderType holder : nsa.getOther()) {
            nsiNsa.getAny().addAll(holder.getAny());
        }

        // The network ResourceRefType is populated after the corresponding
        // networks are created.
        
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
