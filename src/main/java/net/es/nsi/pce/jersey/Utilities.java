/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.jersey;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author hacksaw
 */
public class Utilities {
    public static Map<String, String> getNameSpace() {
        Map<String, String> namespacePrefixMapper = new HashMap<>(4);
        namespacePrefixMapper.put("http://schemas.es.net/nsi/2013/08/pce/messages", "m");
        namespacePrefixMapper.put("http://schemas.ogf.org/nsi/2013/07/services/point2point", "p");
        namespacePrefixMapper.put("http://schemas.es.net/nsi/2013/07/topology/types", "t");
        namespacePrefixMapper.put("http://schemas.ogf.org/nsi/2013/12/services/definition", "s");
        namespacePrefixMapper.put("http://schemas.es.net/nsi/2013/07/management/types", "o");
        return namespacePrefixMapper;
    }
    
    public static boolean validMediaType(String mediaType) {
        HashSet<String> mediaTypes = new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(MediaType.APPLICATION_JSON);
                add(MediaType.APPLICATION_XML);
                add(MediaType.APPLICATION_XML);
                add("application/vnd.net.es.pce.v1+json");
                add("application/vnd.net.es.pce.v1+xml");
            }
        };

        return mediaTypes.contains(mediaType);
    }
    
    public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
        if (time <= 0) {
            return null;
        }
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        return newXMLGregorianCalendar;
    }
}
