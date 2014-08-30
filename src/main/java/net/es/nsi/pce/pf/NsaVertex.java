package net.es.nsi.pce.pf;

import net.es.nsi.pce.topology.jaxb.NsaType;

/**
 * A vertex for the directed control plane graph.
 *
 * @author hacksaw
 */
public class NsaVertex extends GraphVertex {
    private NsaType nsa = null;

    public NsaVertex(String id, NsaType nsa) {
        super(id);
        this.nsa = nsa;
    }

    /**
     * @return the nsa
     */
    public NsaType getNsa() {
        return nsa;
    }

    @Override
    public String toString() {
        return this.getId();
    }
}
