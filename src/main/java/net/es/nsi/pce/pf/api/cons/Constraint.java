package net.es.nsi.pce.pf.api.cons;

/**
 * Defines an abstract class for PCE constraints.
 * 
 * @author hacksaw
 */
public abstract class Constraint {
    private boolean inverse;

    public boolean isInverse() {
        return inverse;
    }

    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }

}
