/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.jaxb.topology.StpType;

/**
 *
 * @author hacksaw
 */
public class StpVertex extends GraphVertex {
    private StpType stp = null;

    public StpVertex(String id, StpType stp) {
        super(id);
        this.stp = stp;
    }

    /**
     * @return the stp
     */
    public StpType getStp() {
        return stp;
    }

    @Override
    public String toString() {
        return "StpVertex=" + stp.getId();
    }
}
