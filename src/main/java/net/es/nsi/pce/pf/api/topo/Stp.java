package net.es.nsi.pce.pf.api.topo;


public class Stp extends TopoObject {
    private Network network;
    private String localId;
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
}
