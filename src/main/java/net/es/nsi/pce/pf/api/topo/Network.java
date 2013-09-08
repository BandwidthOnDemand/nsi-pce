package net.es.nsi.pce.pf.api.topo;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Network extends TopologyObject {
    private String networkId;
    private HashMap<String, Stp> stps = new HashMap<String, Stp>();
    private Set<StpConnection> stpConnections = new HashSet<StpConnection>();

    public void put(String stpId, Stp stp) {
        stps.put(stpId, stp);
    }
    public Stp getStp(String stpId) {
        return stps.get(stpId);
    }
    public Set<String> getStpIds() {
        return stps.keySet();
    }

    public Set<StpConnection> getStpConnections() {
        return stpConnections;
    }
    public Set<StpConnection> getConnectionsFrom(Stp stp) {
        HashSet<StpConnection> result = new HashSet<StpConnection>();
        for (StpConnection conn : stpConnections) {
            if (conn.getA().equals(stp)) {
                result.add(conn);
            }
        }
        return result;
    }


    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }
}
