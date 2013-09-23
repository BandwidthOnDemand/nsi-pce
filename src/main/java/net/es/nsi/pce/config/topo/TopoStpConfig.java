package net.es.nsi.pce.config.topo;

/**
 * 
 * @author hacksaw
 */
public class TopoStpConfig {
    private String localId;
    private String remoteNetworkId;
    private String remoteLocalId;
    private Integer cost;
    
    public TopoStpConfig() {

    }

    /**
     * @return the localId
     */
    public String getLocalId() {
        return localId;
    }

    /**
     * @param localId the localId to set
     */
    public void setLocalId(String localId) {
        this.localId = localId;
    }

    /**
     * @return the remoteNetworkId
     */
    public String getRemoteNetworkId() {
        return remoteNetworkId;
    }

    /**
     * @param remoteNetworkId the remoteNetworkId to set
     */
    public void setRemoteNetworkId(String remoteNetworkId) {
        this.remoteNetworkId = remoteNetworkId;
    }

    /**
     * @return the remoteLocalId
     */
    public String getRemoteLocalId() {
        return remoteLocalId;
    }

    /**
     * @param remoteLocalId the remoteLocalId to set
     */
    public void setRemoteLocalId(String remoteLocalId) {
        this.remoteLocalId = remoteLocalId;
    }

    /**
     * @return the cost
     */
    public Integer getCost() {
        return cost;
    }

    /**
     * @param cost the cost to set
     */
    public void setCost(Integer cost) {
        this.cost = cost;
    }
}
