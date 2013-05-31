package net.es.nsi.pce.pf.api.topo;


import java.util.HashMap;
import java.util.Set;

public class Network extends TopoObject {
    private String networkId;
    private HashMap<String, Stp> stps = new HashMap<String, Stp>();

    public void put(String stpId, Stp stp) {
        stps.put(stpId, stp);
    }
    public Stp getStp(String stpId) {
        return stps.get(stpId);
    }
    public Set<String> getStpIds() {
        return stps.keySet();
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }
}
