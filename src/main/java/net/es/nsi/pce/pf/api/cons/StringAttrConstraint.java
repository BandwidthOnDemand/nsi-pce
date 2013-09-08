package net.es.nsi.pce.pf.api.cons;

/**
 * Extends the basic attribute name PCE constraint to model an attribute
 * name/value pair.
 * 
 * @author hacksaw
 */
public abstract class StringAttrConstraint extends AttrConstraint {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
