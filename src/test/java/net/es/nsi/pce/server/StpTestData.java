/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.server;

import net.es.nsi.pce.api.jaxb.StpType;

/**
 *
 * @author hacksaw
 */
public class StpTestData {
    private StpType stpA = new StpType();
    private int vlanA;
    private StpType stpZ = new StpType();
    private int vlanZ;

    /**
     * @return the stpA
     */
    public StpType getStpA() {
        return stpA;
    }

    /**
     * @param stpA the stpA to set
     */
    public void setStpA(StpType stpA) {
        this.stpA = stpA;
    }

    /**
     * @return the vlanA
     */
    public int getVlanA() {
        return vlanA;
    }

    /**
     * @param vlanA the vlanA to set
     */
    public void setVlanA(int vlanA) {
        this.vlanA = vlanA;
    }

    /**
     * @return the stpZ
     */
    public StpType getStpZ() {
        return stpZ;
    }

    /**
     * @param stpZ the stpZ to set
     */
    public void setStpZ(StpType stpZ) {
        this.stpZ = stpZ;
    }

    /**
     * @return the vlanZ
     */
    public int getVlanZ() {
        return vlanZ;
    }

    /**
     * @param vlanZ the vlanZ to set
     */
    public void setVlanZ(int vlanZ) {
        this.vlanZ = vlanZ;
    }
    
}
