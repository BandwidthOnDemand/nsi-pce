package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
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
     */
    public Topology getTopology();
    
    /**
     * Get a list of network identifiers defined within topology.
     * 
     * @return A set of network identifiers.
     */
    public Set<String> getNetworkIds();
    
    /**
     * Get a list of Network objects defined in topology.
     * 
     * @return List of Network objects.
     */
    public Collection<Network> getNetworks();

    /**
     * Get the audit interval (in milliseconds) used by scheduler to schedule
     * a repeating audit of topology.
     * 
     * @return A long representing the auditTime in milliseconds. 
     */
    public long getAuditInterval();
    
    /**
     * Set the configuration for NSI topology discovery.  The context of the
     * configuration string is based on the type of topology provider.
     * 
     * @param configuration The source from which to load NSI topology.
     */
    public void setConfiguration(String configuration);

    /**
     * Initialize the NSI topology provider.
     * 
     * @Exception Will throw an exception if the provider cannot be initialized.
     */
    public void initialize() throws Exception;
    
    /**
     * Get the configuration for NSI topology discovery.
     * 
     * @return A String identifying the configuration source of NSI topology.
     */
    public String getConfiguration();
    
    /**
     * Load topology from the defined source.  This is also invoked by the
     * PCE schedule to perform an audit.
     * 
     * @throws Exception If topology discovery fails.
     */
    public void loadTopology() throws Exception;
}
