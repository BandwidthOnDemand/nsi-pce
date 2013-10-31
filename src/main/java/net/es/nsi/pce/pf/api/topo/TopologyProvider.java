package net.es.nsi.pce.pf.api.topo;

import java.util.Set;

/**
 * Interface definition for an NSI Topology provider.
 * 
 * @author hacksaw
 */

public interface TopologyProvider {
    /**
     * Get the discovered NSI network topology.  Will load a copy of topology
     * if it has not yet been loaded.
     * 
     * @return The discovered NSI topology.
     * @throws Exception If an error occurs during discovery.
     */
    public Topology getTopology() throws Exception;
    
    /**
     * Get a list of network identifiers defined within topology.
     * 
     * @return A set of network identifiers.
     */
    public Set<String> getNetworkIds();

    /**
     * Get the audit interval (in milliseconds) used by scheduler to schedule
     * a repeating audit of topology.
     * 
     * @return A long representing the auditTime in milliseconds. 
     */
    public long getAuditInterval();
    
    /**
     * Set the source for NSI topology discovery.  The context of the source is
     * based on the type of topology provider.
     * 
     * @param source The source from which to load NSI topology.
     */
    public void setTopologySource(String source);
    
    /**
     * Get the source for NSI topology discovery.
     * 
     * @return A String identifying the source of NSI topology.
     */
    public String getTopologySource();
    
    /**
     * Load topology from the defined source.  This is also invoked by the
     * PCE schedule to perform an audit.
     * 
     * @throws Exception If topology discovery fails.
     */
    public void loadTopology() throws Exception;
}
