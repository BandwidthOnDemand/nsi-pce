package net.es.nsi.pce.pf.api.cons;


public abstract class Constraint {
    private boolean inverse;

    public boolean isInverse() {
        return inverse;
    }

    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }

}
