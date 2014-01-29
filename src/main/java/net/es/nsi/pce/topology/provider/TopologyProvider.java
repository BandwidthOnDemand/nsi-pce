package net.es.nsi.pce.topology.provider;

import java.util.Collection;
import net.es.nsi.pce.managemenet.jaxb.TopologyStatusType;
import net.es.nsi.pce.topology.model.NsiTopology;

/**
 * Interface definition for an NSI Topology provider.
 * 
 * @author hacksaw
 */

public interface TopologyProvider {
    /**
     * Initialize the NSI topology provider.
     * 
     * @Exception Will throw an exception if the provider cannot be initialized.
     */
    public void initialize() throws Exception;
    
    /**
     * Get the discovered NSI network topology.  Will load a copy of topology
     * if it has not yet been loaded.
     * 
     * @return The discovered NSI topology.
     */
    public NsiTopology getTopology();

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
     * Get the configuration for NSI topology discovery.
     * 
     * @return A String identifying the configuration source of NSI topology.
     */
    public String getConfiguration();
    
    /**
     * Load topology from the local cache source.
     * 
     * @throws Exception If topology discovery fails.
     */
    public void loadCache() throws Exception;
    
    /**
     * Saves a current copy of topology to the local cache.
     * 
     * @throws Exception If topology discovery fails.
     */
    public void saveCache() throws Exception;
    
    /**
     * Load topology from the defined source.  This is also invoked by the
     * PCE schedule to perform an audit.
     * 
     * @throws Exception If topology discovery fails.
     */
    public void loadTopology() throws Exception;
    
    public long getLastAudit();
    
    public long getLastModified();
    
    /**
     * 
     * @return The current status of the topology provider.
     */
    public TopologyStatusType getSummaryStatus();
    
    public TopologyProviderStatus getManifestStatus();
    
    public Collection<TopologyProviderStatus> getProviderStatus();
    
}
