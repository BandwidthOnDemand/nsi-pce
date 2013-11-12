package net.es.nsi.pce.pf.api;

import net.es.nsi.pce.topology.jaxb.StpType;

/**
 * Defines a pair of STP (A and Z end) used in the PCE path composition.
 * 
 * @author hacksaw
 */
public class StpPair implements Cloneable {
    private StpType a;
    private StpType z;

    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof StpType) ) return false;

        StpPair that = (StpPair) other;
        return (
                this.getA().equals(that.getA()) &&
                this.getZ().equals(that.getZ()) );

    }


    public StpType getA() {
        return a;
    }

    public void setA(StpType a) {
        this.a = a;
    }

    public StpType getZ() {
        return z;
    }

    public void setZ(StpType z) {
        this.z = z;
    }
    @Override
    public String toString() {
        return a.toString() + " -- " + z.toString();
    }
    @Override
    public StpPair clone() {
        StpPair foo = new StpPair();
        foo.setA(a);
        foo.setZ(z);

        return foo;
    }

}
