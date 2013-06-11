package net.es.nsi.pce.pf.api.cons;


public class TopoPathEndpoints extends TopoConstraint {
    private String srcLocal;
    private String srcNetwork;

    private String dstLocal;
    private String dstNetwork;

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
}
