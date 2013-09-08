/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

/**
 *
 * @author hacksaw
 */
public class SymmetricPathConstraint extends AttrConstraint {
    public static final String SYMMETRICPATH = "symmetricPath";
    private boolean symmetricPath = false;

    public SymmetricPathConstraint() {   
        this.setAttrName(SYMMETRICPATH);
    }
        
    /**
     * @return the value
     */
    public boolean isSymmetricPath() {
        return symmetricPath;
    }

    /**
     * @param value the value to set
     */
    public void setSymmetricPath(boolean symmetricPath) {
        this.symmetricPath = symmetricPath;
    }
    
    
}
