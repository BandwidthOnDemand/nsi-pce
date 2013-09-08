/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.jersey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author hacksaw
 */
public class Utilities {
    public static Map<String, String> getNameSpace() {
        Map<String, String> namespacePrefixMapper = new HashMap<String, String>(2);
        namespacePrefixMapper.put("http://schemas.es.net/nsi/2013/08/pce/messages", "m");
        namespacePrefixMapper.put("http://schemas.ogf.org/nsi/2013/07/services/point2point", "p");
        return namespacePrefixMapper;
    }
    
    public static boolean validMediaType(String mediaType) {
        HashSet<String> mediaTypes = new HashSet() {
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
}
