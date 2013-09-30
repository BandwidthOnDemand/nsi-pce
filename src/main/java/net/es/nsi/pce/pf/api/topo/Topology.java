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
    private ConcurrentHashMap<String, Nsa> nsas = new ConcurrentHashMap<String, Nsa>();
    private ConcurrentHashMap<String, Network> networks = new ConcurrentHashMap<String, Network>();
    private ConcurrentHashMap<String, Sdp> sdpLinks = new ConcurrentHashMap<String, Sdp>();
    
    public Nsa getNsaByNetwork(Network network) {
        return nsas.get(network.getNsaId());
    }
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public Network getNetworkById(String networkId) {
        return networks.get(networkId);
    }
    
    /**
     * Add a Network object to the topology indexed by networkId.
     * 
     * @param net The Network object to store.
     */
    public void addNetwork(Network net) {
        networks.put(net.getId(), net);

    }

    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public Network getNetworkByName(String name) {
        for (Network network : networks.values()) {
            if (name.contentEquals(network.getName())) {
                return network;
            }
        }
        
        return null;
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
    
    /**
     * @param nsa the nsa to set
     */
    public void addNsa(Nsa nsa) {
        this.getNsas().put(nsa.getId(), nsa);
    }

    /**
     * @return the nsas
     */
    public ConcurrentHashMap<String, Nsa> getNsas() {
        return nsas;
    }

    /**
     * @param nsas the nsas to set
     */
    public void setNsas(ConcurrentHashMap<String, Nsa> nsas) {
        this.nsas = nsas;
    }
}
