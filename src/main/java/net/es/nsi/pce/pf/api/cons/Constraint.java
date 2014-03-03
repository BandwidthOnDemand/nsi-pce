package net.es.nsi.pce.pf.api.cons;

/**
 * Defines an abstract class for PCE constraints.
 * 
 * @author hacksaw
 */
public abstract class Constraint {
    private boolean inverse = false;

    /**
     * Returns true if the constraint is inverse (or a NOT) to the value
     * provided, and false otherwise.
     * 
     * @return true if the constraint is inverse to the value provided, or
     * false otherwise.
     */
    public boolean isInverse() {
        return inverse;
    }

    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }
}
