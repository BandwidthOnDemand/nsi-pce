package net.es.nsi.pce.pf.api.cons;

/**
 * Defines a basic PCE constraint modeling an number attribute with a name and
 * Long value.
 * 
 * @author hacksaw
 */
public class BooleanAttrConstraint extends AttrConstraint {
    private boolean value;

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
}

