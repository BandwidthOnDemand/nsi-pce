/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

/**
 *
 * @author hacksaw
 */
public class MtuConstraint extends NumAttrConstraint {
    public static final String MTU = "mtu";
    
    public MtuConstraint() {   
        this.setAttrName(MTU);
    }
}
