/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 *
 * @author hacksaw
 */
public class TestURLEncode {
    public static void main(String[] args) throws Exception {
        String url = "application/vnd.ogf.nsi.topology.v2+xml";
        System.out.println(url);
        url = URLEncoder.encode(url, "UTF-8");
        System.out.println(url);
        url = URLDecoder.decode(url, "UTF-8");
        System.out.println(url);
    }
}
