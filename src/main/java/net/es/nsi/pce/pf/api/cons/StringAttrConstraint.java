package net.es.nsi.pce.pf.api.cons;

public abstract class StringAttrConstraint extends AttrConstraint {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
