package net.es.nsi.pce.pf.graph;

import net.es.nsi.pce.jaxb.topology.NsaType;

/**
 * An edge for the directed control plane graph.
 * 
 * @author hacksaw
 */
public class NsaEdge extends GraphEdge {
    private NsaType sourceNsa;
    private NsaType destinationNsa;

    public NsaEdge(NsaType sourceNsa, NsaType destinationNsa) {
        super(sourceNsa.getId() + "--" + destinationNsa.getId());
        this.sourceNsa = sourceNsa;
        this.destinationNsa = destinationNsa;
    }

    /**
     * @return the sourceNsa
     */
    public NsaType getSourceNsa() {
        return sourceNsa;
    }

    /**
     * @return the destinationNsa
     */
    public NsaType getDestinationNsa() {
        return destinationNsa;
    }

    @Override
    public String toString() {
        return this.getId();
    }
}
