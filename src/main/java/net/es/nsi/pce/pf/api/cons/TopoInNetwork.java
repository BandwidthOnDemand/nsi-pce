package net.es.nsi.pce.pf.api.cons;

/**
 * 
 * @author hacksaw
 */
public class TopoInNetwork extends TopoConstraint {
    private String networkId;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }
}
