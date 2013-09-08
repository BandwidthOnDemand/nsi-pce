package net.es.nsi.pce.pf.api.topo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.es.nsi.pce.topology.jaxb.LabelGroupType;

/**
 * Defines the NSI CS STP construct used within Topology graph.
 * 
 * @author hacksaw
 */
public class Stp extends TopologyObject {
    private Network network;
    private String localId;
    private ArrayList<LabelGroupType> labelGroups = new ArrayList<LabelGroupType>();
    
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (! (other instanceof Stp) ) {
            return false;
        }

        Stp that = (Stp) other;
        if (!this.getNetwork().equals(that.getNetwork())) {
            return false;
        }
        else if (!this.getLocalId().equals(that.getLocalId())) {
            return false;
        }
        
        Set<LabelGroupType> thisSet = new HashSet<LabelGroupType>();
        thisSet.addAll(this.getLabelGroups());
        Set<LabelGroupType> thatSet = new HashSet<LabelGroupType>();
        thatSet.addAll(that.getLabelGroups());
        if (!thisSet.equals(thatSet)) {
            return false;
        }
        
        return true;

    }
    
    @Override
    public String toString() {
        return network.getNetworkId()+"::"+localId;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    /**
     * @return the labels
     */
    public ArrayList<LabelGroupType> getLabelGroups() {
        return labelGroups;
    }

    /**
     * @param labels the labels to set
     */
    public void setLableGroups(ArrayList<LabelGroupType> labelGroups) {
        this.labelGroups = labelGroups;
    }
}
