package net.es.nsi.pce.pf.api.topo;

import java.util.Objects;
import net.es.nsi.pce.config.topo.nml.Directionality;

/**
 * Defines the NSI CS STP construct used within Topology graph.
 * 
 * @author hacksaw
 */
public class Stp extends TopologyObject {
    private String networkId;
    private String localId;
    private Directionality directionality;
    private int vlanId;
    
    // Links to the remote STP as derived from topology.
    private Stp remoteStp;

    public String getId() {
        return localId + ":vlan=" + vlanId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    /**
     * @return the networkId
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @param networkId the networkId to set
     */
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * @return the unidirectional
     */
    public boolean isUnidirectional() {
        return (directionality == Directionality.unidirectional);
    }

    public boolean isBidirectional() {
        return (directionality == Directionality.bidirectional);
    }
        
    /**
     * @return the directionality
     */
    public Directionality getDirectonality() {
        return directionality;
    }

    /**
     * @param directionality the directionality to set
     */
    public void setDirectonality(Directionality directionality) {
        this.directionality = directionality;
    }

    /**
     * @return the vlanId
     */
    public int getVlanId() {
        return vlanId;
    }

    /**
     * @param vlanId the vlanId to set
     */
    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
    }

    /**
     * @return the remoteStp
     */
    public Stp getRemoteStp() {
        return remoteStp;
    }

    /**
     * @param remoteStp the remoteStp to set
     */
    public void setRemoteStp(Stp remoteStp) {
        this.remoteStp = remoteStp;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (! (other instanceof Stp) ) {
            return false;
        }

        Stp that = (Stp) other;
        if (!this.getNetworkId().equalsIgnoreCase(that.getNetworkId())) {
            return false;
        }
        else if (!this.getLocalId().equalsIgnoreCase(that.getLocalId())) {
            return false;
        }
        
        if (this.vlanId != that.getVlanId()) {
            return false;
        }
        
        return true;

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.networkId);
        hash = 23 * hash + Objects.hashCode(this.localId);
        hash = 23 * hash + Objects.hashCode(this.directionality);
        hash = 23 * hash + this.vlanId;
        return hash;
    }
    
    @Override
    public String toString() {
        return getId();
    }
}
