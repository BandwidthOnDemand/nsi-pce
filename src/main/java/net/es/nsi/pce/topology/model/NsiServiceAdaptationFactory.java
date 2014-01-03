package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ServiceAdaptationType;
import org.apache.http.client.utils.DateUtils;

/**
 * A factory class for generating NSI ServiceDomain resource objects.
 * 
 * @author hacksaw
 */
public class NsiServiceAdaptationFactory {
    
    private static final String NSI_ROOT_SERVICEADAPTATIONS = "/topology/serviceAdaptations/";
    
    public static List<ServiceAdaptationType> ifModifiedSince(String ifModifiedSince, List<ServiceAdaptationType> serviceAdaptationList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<ServiceAdaptationType> iter = serviceAdaptationList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }
        
        return serviceAdaptationList;
    }
}
