/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo.nml;

/**
 *
 * @author hacksaw
 */
public class BidirectionalEthernetPort extends EthernetPort {
    
    private EthernetPort outbound;
    private EthernetPort inbound;
    private BidirectionalEthernetPort remotePort;

    /**
     * @return the outbound
     */
    public EthernetPort getOutbound() {
        return outbound;
    }

    /**
     * @param outbound the outbound to set
     */
    public void setOutbound(EthernetPort outbound) {
        this.outbound = outbound;
    }

    /**
     * @return the inbound
     */
    public EthernetPort getInbound() {
        return inbound;
    }

    /**
     * @param inbound the inbound to set
     */
    public void setInbound(EthernetPort inbound) {
        this.inbound = inbound;
    }

    /**
     * @return the remotePort
     */
    public BidirectionalEthernetPort getRemotePort() {
        return remotePort;
    }

    /**
     * @param remotePort the remotePort to set
     */
    public void setRemotePort(BidirectionalEthernetPort remotePort) {
        this.remotePort = remotePort;
    }
    
}
