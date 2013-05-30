package net.es.nsi.pce.pf.api.topo;

import java.util.HashMap;
import java.util.Set;

public class Topology {
    private HashMap<String, Network> networks = new HashMap<String, Network>();
    public Network getNetwork(String networkId) {
        return networks.get(networkId);
    }
    public void setNetwork(String networkId, Network net) {
        networks.put(networkId, net);

    }
    public Set<String> getNetworkIds() {
        return networks.keySet();
    }




}
