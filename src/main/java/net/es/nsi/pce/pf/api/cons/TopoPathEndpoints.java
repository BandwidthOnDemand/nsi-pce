package net.es.nsi.pce.pf.api.cons;

import java.util.ArrayList;
import net.es.nsi.pce.topology.jaxb.LabelType;

/**
 * Defines a PCEData constraint for source and destination STP for the path.
 * 
 * @author hacksaw
 */
public class TopoPathEndpoints extends TopoConstraint {
    private String srcLocal;
    private String srcNetwork;
    private ArrayList<LabelType> srcLabels = new ArrayList<LabelType>();
    
    private String dstLocal;
    private String dstNetwork;
    private ArrayList<LabelType> dstLabels = new ArrayList<LabelType>();
    
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
     * @return the srcLabels
     */
    public ArrayList<LabelType> getSrcLabels() {
        return srcLabels;
    }

    /**
     * @param srcLabels the srcLabels to set
     */
    public void setSrcLabels(ArrayList<LabelType> srcLabels) {
        this.srcLabels = srcLabels;
    }

    /**
     * @return the dstLabels
     */
    public ArrayList<LabelType> getDstLabels() {
        return dstLabels;
    }

    /**
     * @param dstLabels the dstLabels to set
     */
    public void setDstLabels(ArrayList<LabelType> dstLabels) {
        this.dstLabels = dstLabels;
    }
}
