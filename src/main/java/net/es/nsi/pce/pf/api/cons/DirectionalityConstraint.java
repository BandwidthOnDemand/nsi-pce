/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

import net.es.nsi.pce.api.jaxb.DirectionalityType;

/**
 *
 * @author hacksaw
 */
public class DirectionalityConstraint extends AttrConstraint {
    public static final String DIRECTIONALITY = "directionality";
    
    private DirectionalityType value = DirectionalityType.BIDIRECTIONAL;

    public DirectionalityConstraint() {   
        this.setAttrName(DIRECTIONALITY);
    }
    
    /**
     * @return the value
     */
    public DirectionalityType getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(DirectionalityType value) {
        this.value = value;
    }
}
