/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

/**
 *
 * @author hacksaw
 */
public class ObjectAttrConstraint extends AttrConstraint {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public <T extends Object> T getValue(Class<T> classOfT) {
        return classOfT.cast(value);
    }
}
