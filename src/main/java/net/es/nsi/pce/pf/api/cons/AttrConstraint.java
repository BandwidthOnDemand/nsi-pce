package net.es.nsi.pce.pf.api.cons;

/**
 * Defines a basic PCE constraint modeling an attribute with a name.
 * 
 * @author hacksaw
 */
public abstract class AttrConstraint extends Constraint {
    private String attrName;

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }
}
