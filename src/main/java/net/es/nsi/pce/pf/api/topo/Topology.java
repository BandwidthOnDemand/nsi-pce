package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Topology is a map of Network objects indexed by networkId, and a map of
 * SDP interconnecting these Networks.
 * 
 * @author hacksaw
 */
public class Topology {
    private ConcurrentHashMap<String, Network> networks = new ConcurrentHashMap<String, Network>();
    private ConcurrentHashMap<String, Sdp> sdpLinks = new ConcurrentHashMap<String, Sdp>();
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public Network getNetwork(String networkId) {
        return networks.get(networkId);
    }
    
    /**
     * Add a Network object to the topology indexed by networkId.
     * 
     * @param networkId The networkId to use as an index.
     * @param net The Network object to store.
     */
    public void setNetwork(String networkId, Network net) {
        networks.put(networkId, net);

    }
    
    /**
     * Get a set of networkIds associated with this topology.
     * 
     * @return Set of networkIds.
     */
    public Set<String> getNetworkIds() {
        return networks.keySet();
    }

    /**
     * Get the list of Network objects associated with this topology.
     * 
     * @return Collection of Network objects from this topology.
     */
    public Collection<Network> getNetworks() {
        return networks.values();
    }
    
    /**
     * Clear the Topology.
     */
    public void clear() {
        networks.clear();
        sdpLinks.clear();
    }

    /**
     * Get a list of SDP in this Topology.
     * 
     * @return List of SDP in a Collection.
     */
    public Collection<Sdp> getSdps() {
        return sdpLinks.values();
    }
    
    /**
     * Get the list of SDP within this topology containing the specified STP
     * (if it exists).
     * 
     * @param stp The STP to locate in the list of SDP.
     * @return The matching list of SDP.
     */
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
     * Get the SDP map associated with this topology.
     * 
     * @return the sdpLinks
     */
    public ConcurrentHashMap<String, Sdp> getSdpLinks() {
        return sdpLinks;
    }

    /**
     * Add an SDP to the map.
     * 
     * @return the sdpLinks
     */
    public Sdp addSdp(Sdp sdp) {
        return sdpLinks.put(sdp.getId(), sdp);
    }
    
    /**
     * Replace the current map of SDP with the provided map.
     * 
     * @param sdpLinks the sdpLinks to set
     */
    public void setSdpLinks(ConcurrentHashMap<String, Sdp> sdpLinks) {
        this.sdpLinks = sdpLinks;
    }
}
