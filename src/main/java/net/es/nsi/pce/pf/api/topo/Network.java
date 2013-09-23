package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.es.nsi.pce.config.topo.nml.EthernetPort;

/**
 * Defines an NSI network object consisting of a network identifier, a list
 * of STP, and a set of SDP links if available.
 * 
 * @author hacksaw
 */
public class Network extends TopologyObject {
    private String name;
    private String networkId;
    private HashMap<String, Stp> stps = new HashMap<String, Stp>();
    private Set<Sdp> sdpLinks = new HashSet<Sdp>();

    public void put(String stpId, Stp stp) {
        stps.put(stpId, stp);
    }
    
    public Stp getStp(String stpId) {
        return stps.get(stpId);
    }
    
    public Collection<Stp> getStps() {
        return stps.values();
    }
        
    public Set<String> getStpIds() {
        return stps.keySet();
    }

    public Set<Sdp> getSdp() {
        return sdpLinks;
    }
    public Set<Sdp> getSdpFrom(Stp stp) {
        HashSet<Sdp> result = new HashSet<Sdp>();
        for (Sdp conn : sdpLinks) {
            if (conn.getA().equals(stp)) {
                result.add(conn);
            }
        }
        return result;
    }
    
    public Stp newStp(EthernetPort port, Integer vlanId) {
        Stp stp = new Stp();
        stp.setLocalId(port.getPortId());
        stp.setNetworkId(getNetworkId());
        stp.setNetwork(this);
        stp.setDirectonality(port.getDirectionality());
        stp.setVlanId(vlanId.intValue());
        return stp;
    }
    
    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
