package net.es.nsi.pce.pf.api.topo;

/**
 * An SDP is a logical link between to connected STP.  An SDP is only created
 * when both an A and Z end STP have connectivity defined.
 * 
 * @author hacksaw
 */
public class Sdp extends TopologyObject {
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
