/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

/**
 *
 * @author hacksaw
 */
public class CapacityConstraint extends NumAttrConstraint {
    public static final String CAPACITY = "capacity";
    
    public CapacityConstraint() {
        this.setAttrName(CAPACITY);
    }
}
