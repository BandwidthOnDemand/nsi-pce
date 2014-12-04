/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.description.api.RestException;

/**
 *
 * @author hacksaw
 */
public class NsiPathURI {
    static final String NSI_ROOT_NSAS = "nsas";
    static final String NSI_ROOT_NETWORKS = "networks";

    public static String getURL(String baseURL, String type, String id) throws WebApplicationException {
        URL url;
        StringBuilder sb = new StringBuilder(baseURL);
        try {
            if (!baseURL.endsWith("/")) {
                sb.append("/");
            }
            sb.append(type);
            sb.append("/");
            sb.append(URLEncoder.encode(id.trim(), "UTF-8"));
            url = new URL(sb.toString());
        } catch (MalformedURLException | UnsupportedEncodingException ex) {
            throw RestException.internalServerErrorException(type, "baseURL=" + baseURL + ", type=" + type + ", id=" + id);
        }

        return url.toString();
    }
}
