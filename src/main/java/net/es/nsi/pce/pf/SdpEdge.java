/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.topology.jaxb.SdpType;

/**
 *
 * @author hacksaw
 */
public class SdpEdge extends GraphEdge {
    private SdpType sdp;

    public SdpEdge(SdpType sdp) {
        super(sdp.getId());
        this.sdp = sdp;
    }

    public SdpEdge(String id, SdpType sdp) {
        super(id);
        this.sdp = sdp;
    }

    /**
     * @return the sdp
     */
    public SdpType getSdp() {
        return sdp;
    }

    /**
     * @param sdp the sdp to set
     */
    public void setSdp(SdpType sdp) {
        this.sdp = sdp;
    }
}
