package net.es.nsi.pce.pf.api;

import net.es.nsi.pce.pf.api.topo.Stp;

public class StpPair implements Cloneable {
    private Stp a;
    private Stp z;

    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof Stp) ) return false;

        StpPair that = (StpPair) other;
        return (
                this.getA().equals(that.getA()) &&
                this.getZ().equals(that.getZ()) );

    }


    public Stp getA() {
        return a;
    }

    public void setA(Stp a) {
        this.a = a;
    }

    public Stp getZ() {
        return z;
    }

    public void setZ(Stp z) {
        this.z = z;
    }
    public String toString() {
        return a.toString()+ " -- "+z.toString();
    }
    public StpPair clone() {
        StpPair foo = new StpPair();
        foo.setA(a);
        foo.setZ(z);

        return foo;
    }

}
