package net.es.nsi.pce.pf.api;

import java.util.Objects;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.SimpleLabel;

/**
 * Defines a pair of STP (A and Z end) used in the PCE path composition.
 *
 * @author hacksaw
 */
public class StpPair implements Cloneable {
    private final StpType a;
    private final SimpleLabel aLabel;
    private final StpType z;
    private final SimpleLabel zLabel;

    public StpPair(StpType a, SimpleLabel aLabel, StpType z, SimpleLabel zLabel) {
        this.a = a;
        this.aLabel = aLabel;
        this.z = z;
        this.zLabel = zLabel;
    }

    public StpType getA() {
        return a;
    }

    public SimpleLabel getaLabel() {
        return aLabel;
    }

    public StpType getZ() {
        return z;
    }

    public SimpleLabel getzLabel() {
        return zLabel;
    }

    @Override
    public String toString() {
        return a.toString() + "?" + aLabel.toString() + " -- " + z.toString() + "?" + zLabel.toString();
    }

    @Override
    public StpPair clone() {
        return new StpPair(a, aLabel, z, zLabel);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof StpType)) return false;

        StpPair that = (StpPair) other;

        if (!this.getA().getId().equalsIgnoreCase(that.getA().getId())) {
            return false;
        }

        if (!this.getZ().getId().equalsIgnoreCase(that.getZ().getId())) {
            return false;
        }

        if (this.aLabel != null && !this.aLabel.equals(that.getaLabel())) {
            return false;
        }

        if (this.zLabel != null && !this.zLabel.equals(that.getaLabel())) {
            return false;
        }

        return this.getA().getId().equalsIgnoreCase(that.getA().getId()) && this.getZ().getId().equalsIgnoreCase(that.getZ().getId());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.a);
        hash = 23 * hash + Objects.hashCode(this.aLabel);
        hash = 23 * hash + Objects.hashCode(this.z);
        hash = 23 * hash + Objects.hashCode(this.zLabel);
        return hash;
    }
}
