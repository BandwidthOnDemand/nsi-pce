/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo.nml;

/**
 *
 * @author hacksaw
 */
public class Relationships {
    // Topology relationship types.
    public final static String hasInboundPort = "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort";
    public final static String hasOutboundPort = "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort";
    public final static String hasService = "http://schemas.ogf.org/nml/2013/05/base#hasService";
    public final static String isAlias = "http://schemas.ogf.org/nml/2013/05/base#isAlias";
    public final static String providesLink = "http://schemas.ogf.org/nml/2013/05/base#providesLink";
    
    public static boolean hasInboundPort(String type) {
        return Relationships.hasInboundPort.equalsIgnoreCase(type);
    }

    public static boolean hasOutboundPort(String type) {
        return Relationships.hasOutboundPort.equalsIgnoreCase(type);
    }
    
    public static boolean hasService(String type) {
        return Relationships.hasService.equalsIgnoreCase(type);
    }    
        
    public static boolean isAlias(String type) {
        return Relationships.isAlias.equalsIgnoreCase(type);
    }
    
    public static boolean providesLink(String type) {
        return Relationships.providesLink.equalsIgnoreCase(type);
    }
}
