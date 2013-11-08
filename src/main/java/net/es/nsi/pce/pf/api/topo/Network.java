package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import net.es.nsi.pce.config.topo.nml.EthernetPort;

/**
 * Defines an NSI network object consisting of a network identifier, a list
 * of STP, and a set of SDP links if available.
 * 
 * @author hacksaw
 */
public class Network extends TopologyObject {
    private String nsaId;
    private String name;
    private String networkId;
    private HashMap<String, Stp> stps = new HashMap<String, Stp>();

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

    
    public Stp newStp(EthernetPort port, Integer vlanId) {
        Stp stp = new Stp();
        stp.setLocalId(port.getPortId());
        stp.setNetworkId(getNetworkId());
        stp.setDirectonality(port.getDirectionality());
        stp.setVlanId(vlanId.intValue());
        return stp;
    }
    
    public String getNetworkId() {
        return networkId;
    }
    
    public String getId() {
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
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (! (other instanceof Network) ) {
            return false;
        }

        Network that = (Network) other;
        if (this.getId().contentEquals(that.getId())) {
            return true;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.networkId);
        return hash;
    }

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }
}
