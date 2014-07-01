/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path.services;

/**
 *
 * @author hacksaw
 */
public class EthernetTypes {
    public static final String NAMESPACE_ETHERNET ="http://schemas.ogf.org/nml/2012/10/ethernet";
    public static final String BURSTSIZE = "http://schemas.ogf.org/nml/2012/10/ethernet#burstsize";
    public static final String MTU = "http://schemas.ogf.org/nml/2012/10/ethernet#mtu";
    public static final String VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";

    private static final Namespace burstsize = new Namespace(NAMESPACE_ETHERNET, BURSTSIZE, "burstsize");
    private static final Namespace mtu = new Namespace(NAMESPACE_ETHERNET, MTU, "mtu");
    private static final Namespace vlan = new Namespace(NAMESPACE_ETHERNET, VLAN, "vlan");

    /**
     * @return the burstsize
     */
    public static Namespace getBurstsize() {
        return burstsize;
    }

    /**
     * @return the mtu
     */
    public static Namespace getMtu() {
        return mtu;
    }

    /**
     * @return the vlan
     */
    public static Namespace getVlan() {
        return vlan;
    }
}
