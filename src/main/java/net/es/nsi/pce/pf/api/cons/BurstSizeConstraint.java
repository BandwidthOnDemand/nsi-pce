/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

/**
 *
 * @author hacksaw
 */
public class BurstSizeConstraint extends NumAttrConstraint {
    public static final String BURSTSIZE = "burstSize";
    
    public BurstSizeConstraint() {   
        this.setAttrName(BURSTSIZE);
    }
}
