package net.es.nsi.pce.pf.api.topo;


import java.util.Set;

public interface TopologyProvider {
    public Topology getTopology() throws Exception;
    public Set<String> getNetworkIds();


    // management functions
    public void setTopologySource(String source);
    public void loadTopology() throws Exception;

}
