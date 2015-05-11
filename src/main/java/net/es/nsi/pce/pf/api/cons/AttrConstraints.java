/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.cons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.es.nsi.pce.path.jaxb.TypeValueType;

/**
 * Models a set of attribute constraints.
 *
 * @author hacksaw
 */
public class AttrConstraints {
    private final Set<Constraint> constraints;

    public AttrConstraints() {
        constraints = new HashSet<>();
    }

    public AttrConstraints(Set<Constraint> constraints) {
        this.constraints = new HashSet<>();
        for (Constraint constraint : constraints) {
            if (constraint instanceof AttrConstraint) {
                this.constraints.add((AttrConstraint) constraint);
            }
        }
    }

    public AttrConstraints(AttrConstraints constraints) {
        this.constraints = new HashSet<>(constraints.get());
    }

    public boolean add(AttrConstraint constraint) {
        return constraints.add(constraint);
    }

    public AttrConstraint remove(AttrConstraint constraint) {
        return removeAttrConstraint(constraint.getAttrName());
    }

    public Set<Constraint> get() {
        return Collections.unmodifiableSet(constraints);
    }

    public AttrConstraint removeAttrConstraint(String name) {
        for (Iterator<Constraint> it = constraints.iterator(); it.hasNext(); ) {
            Constraint constraint = it.next();
            if (constraint instanceof AttrConstraint) {
                if (((AttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                    it.remove();
                    return (AttrConstraint) constraint;
                }
            }
        }
        return null;
    }

    public AttrConstraint getAttrConstraint(String name) {
        for (Constraint constraint : constraints) {
            if (constraint instanceof AttrConstraint && ((AttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                return (AttrConstraint) constraint;
            }
        }
        return null;
    }

    public NumAttrConstraint removeNumAttrConstraint(String name) {
        for (Iterator<Constraint> it = constraints.iterator(); it.hasNext(); ) {
            Constraint constraint = it.next();
            if (constraint instanceof NumAttrConstraint) {
                if (((AttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                    it.remove();
                    return (NumAttrConstraint) constraint;
                }
            }
        }
        return null;
    }

    public NumAttrConstraint getNumAttrConstraint(String name) {
        for (Constraint constraint : constraints) {
            if (constraint instanceof NumAttrConstraint) {
                if (((AttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                    return (NumAttrConstraint) constraint;
                }
            }
        }
        return null;
    }

    public StringAttrConstraint removeStringAttrConstraint(String name) {
        for (Iterator<Constraint> it = constraints.iterator(); it.hasNext(); ) {
            Constraint constraint = it.next();
            if (constraint instanceof StringAttrConstraint &&
                    ((StringAttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                it.remove();
                return (StringAttrConstraint) constraint;
            }
        }
        return null;
    }

    public StringAttrConstraint getStringAttrConstraint(String name) {
        for (Constraint constraint : constraints) {
            if (constraint instanceof StringAttrConstraint) {
                StringAttrConstraint str = (StringAttrConstraint) constraint;

                String attrName = str.getAttrName();
                if (attrName != null && attrName.equalsIgnoreCase(name)) {
                    return (StringAttrConstraint) constraint;
                }
            }
        }
        return null;
    }

    public BooleanAttrConstraint removeBooleanAttrConstraint(String name) {
        for (Iterator<Constraint> it = constraints.iterator(); it.hasNext(); ) {
            Constraint constraint = it.next();
            if (constraint instanceof BooleanAttrConstraint &&
                    ((BooleanAttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                it.remove();
                return (BooleanAttrConstraint) constraint;
            }
        }
        return null;
    }

    public BooleanAttrConstraint getBooleanAttrConstraint(String name) {
        for (Constraint constraint : constraints) {
            if (constraint instanceof BooleanAttrConstraint &&
                    ((BooleanAttrConstraint) constraint).getAttrName().equalsIgnoreCase(name)) {
                return (BooleanAttrConstraint) constraint;
            }
        }
        return null;
    }

    public List<TypeValueType> removeStringAttrConstraints() {
        List<TypeValueType> results = new ArrayList<>();

        for (Iterator<Constraint> it = constraints.iterator(); it.hasNext(); ) {
            Constraint constraint = it.next();
            if (constraint instanceof StringAttrConstraint) {
                it.remove();
                StringAttrConstraint attr = (StringAttrConstraint) constraint;
                TypeValueType tvp = new TypeValueType();
                tvp.setType(attr.getAttrName());
                tvp.setValue(attr.getValue());
                results.add(tvp);
            }
        }
        return results;
    }

    public List<Constraint> getStringAttrConstraints() {
        List<Constraint> results = new ArrayList<>();

        for (Constraint constraint : constraints) {
            if (constraint instanceof StringAttrConstraint) {
                results.add(constraint);
            }
        }
        return results;
    }
}
