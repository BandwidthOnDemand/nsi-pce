package net.es.nsi.pce.pf.api;

import net.es.nsi.pce.jaxb.topology.StpType;

/**
 * Defines a pair of STP (A and Z end) used in the PCE path composition.
 *
 * @author hacksaw
 */
public class StpPair implements Cloneable {
    private final StpType a;
    private final StpType z;

    public StpPair(StpType a, StpType z) {
        this.a = a;
        this.z = z;
    }

    public StpType getA() {
        return a;
    }

    public StpType getZ() {
        return z;
    }

    @Override
    public String toString() {
        return a.toString() + " -- " + z.toString();
    }

    @Override
    public StpPair clone() {
        return new StpPair(a, z);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof StpType)) return false;

        StpPair that = (StpPair) other;
        return this.getA().equals(that.getA()) && this.getZ().equals(that.getZ());
    }
}
