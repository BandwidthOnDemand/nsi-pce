package net.es.nsi.pce.pf.api.topo;

import net.es.nsi.pce.config.topo.nml.Directionality;

/**
 * An SDP is a logical link between to connected STP.  An SDP is only created
 * when both an A and Z end STP have connectivity defined.
 * 
 * @author hacksaw
 */
public class Sdp extends TopologyObject {
    private Stp a;
    private Stp z;
    private Directionality directionality;

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

    public String getId() {
        return a.getId() + "::" + z.getId();
    }  

    @Override
    public String toString() {
        return this.getId();
    }

    /**
     * @return the directionality
     */
    public Directionality getDirectionality() {
        return directionality;
    }

    /**
     * @param directionality the directionality to set
     */
    public void setDirectionality(Directionality directionality) {
        this.directionality = directionality;
    }
}
