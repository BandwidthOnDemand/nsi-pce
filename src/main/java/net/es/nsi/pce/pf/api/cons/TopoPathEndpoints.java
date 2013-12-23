package net.es.nsi.pce.pf.api.cons;

import net.es.nsi.pce.topology.jaxb.NmlLabelType;

/**
 * Defines a PCEData constraint for source and destination STP for the path.
 * 
 * @author hacksaw
 */
public class TopoPathEndpoints extends TopoConstraint {
    private String srcLocal;
    private String srcNetwork;
    private NmlLabelType srcLabel;
    
    private String dstLocal;
    private String dstNetwork;
    private NmlLabelType dstLabel;
    
    public String getSrcLocal() {
        return srcLocal;
    }

    public void setSrcLocal(String srcLocal) {
        this.srcLocal = srcLocal;
    }

    public String getSrcNetwork() {
        return srcNetwork;
    }

    public void setSrcNetwork(String srcNetwork) {
        this.srcNetwork = srcNetwork;
    }

    public String getDstLocal() {
        return dstLocal;
    }

    public void setDstLocal(String dstLocal) {
        this.dstLocal = dstLocal;
    }

    public String getDstNetwork() {
        return dstNetwork;
    }

    public void setDstNetwork(String dstNetwork) {
        this.dstNetwork = dstNetwork;
    }

    /**
     * @return the srcLabel
     */
    public NmlLabelType getSrcLabel() {
        return srcLabel;
    }

    /**
     * @param srcLabel the srcLabels to set
     */
    public void setSrcLabel(NmlLabelType srcLabel) {
        this.srcLabel = srcLabel;
    }

    /**
     * @return the dstLabel
     */
    public NmlLabelType getDstLabel() {
        return dstLabel;
    }

    /**
     * @param dstLabels the dstLabels to set
     */
    public void setDstLabel(NmlLabelType dstLabel) {
        this.dstLabel = dstLabel;
    }
}
