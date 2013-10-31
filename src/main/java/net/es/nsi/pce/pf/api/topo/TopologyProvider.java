package net.es.nsi.pce.pf.api.topo;

import java.util.Set;

/**
 * Interface definition for an NSI Topology provider.
 * 
 * @author hacksaw
 */

public interface TopologyProvider {
    // The audit interval used to schedule a audit of topology.
    public long getAuditInterval();
    
    // Get topology.  Will load a copy of topology if it has not yet been loaded.
    public Topology getTopology() throws Exception;
    
    // Get a list of network identifiers defined within topology.
    public Set<String> getNetworkIds();

    // Management functions
    
    // Set the file holding the topology definition.
    public void setTopologySource(String source);
    
    // Get the file holding the topology definition.
    public String getTopologySource();
    
    // Load topology from the defined source file.
    public void loadTopology() throws Exception;
}
