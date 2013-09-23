package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Topology is a map of Network objects indexed by networkId.
 * 
 * @author hacksaw
 */
public class Topology {
    private ConcurrentHashMap<String, Network> networks = new ConcurrentHashMap<String, Network>();
    
    public Network getNetwork(String networkId) {
        return networks.get(networkId);
    }
    public void setNetwork(String networkId, Network net) {
        networks.put(networkId, net);

    }
    public Set<String> getNetworkIds() {
        return networks.keySet();
    }

    public Collection<Network> getNetworks() {
        return networks.values();
    }
    
    public void clear() {
        networks.clear();
    }


}
