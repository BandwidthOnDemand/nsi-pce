package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Topology is a map of Network objects indexed by networkId.
 * 
 * @author hacksaw
 */
public class Topology {
    private ConcurrentHashMap<String, Network> networks = new ConcurrentHashMap<String, Network>();
    private ConcurrentHashMap<String, Sdp> sdpLinks = new ConcurrentHashMap<String, Sdp>();
    
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
        sdpLinks.clear();
    }

    public Collection<Sdp> getSdps() {
        return sdpLinks.values();
    }
        
    public Set<Sdp> getSdpMember(Stp stp) {
        HashSet<Sdp> result = new HashSet<Sdp>();
        for (Sdp conn : sdpLinks.values()) {
            if (conn.getA().equals(stp) || conn.getZ().equals(stp)) {
                result.add(conn);
            }
        }
        return result;
    }

    /**
     * @return the sdpLinks
     */
    public ConcurrentHashMap<String, Sdp> getSdpLinks() {
        return sdpLinks;
    }

    /**
     * @return the sdpLinks
     */
    public Sdp addSdp(Sdp sdp) {
        return sdpLinks.put(sdp.getId(), sdp);
    }
    
    /**
     * @param sdpLinks the sdpLinks to set
     */
    public void setSdpLinks(ConcurrentHashMap<String, Sdp> sdpLinks) {
        this.sdpLinks = sdpLinks;
    }
}
