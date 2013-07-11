package net.es.nsi.pce.pf.api.topo;


public class Stp extends TopoObject {
    private Network network;
    private String localId;
    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof Stp) ) return false;

        Stp that = (Stp) other;
        return (
                this.getNetwork().equals(that.getNetwork()) &&
                this.getLocalId().equals(that.getLocalId()) );

    }
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
