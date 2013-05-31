package net.es.nsi.pce.pf.api;

import net.es.nsi.pce.pf.api.topo.Stp;

public class StpPair {
    private Stp a;
    private Stp z;

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
}
