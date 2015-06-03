/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path;

import net.es.nsi.pce.path.jaxb.StpListType;

/**
 *
 * @author hacksaw
 */
public class StpTestData {
    private String stpA;
    private String stpZ;
    private StpListType ero;

    /**
     * @return the stpA
     */
    public String getStpA() {
        return stpA;
    }

    /**
     * @param stpA the stpA to set
     */
    public void setStpA(String stpA) {
        this.stpA = stpA;
    }

    /**
     * @return the stpZ
     */
    public String getStpZ() {
        return stpZ;
    }

    /**
     * @param stpZ the stpZ to set
     */
    public void setStpZ(String stpZ) {
        this.stpZ = stpZ;
    }

    /**
     * @return the ero
     */
    public StpListType getEro() {
        return ero;
    }

    /**
     * @param ero the ero to set
     */
    public void setEro(StpListType ero) {
        this.ero = ero;
    }
}
