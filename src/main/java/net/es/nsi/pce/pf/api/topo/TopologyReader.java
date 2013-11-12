/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.topo;

import java.util.Map;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.nml.jaxb.NSAType;
import net.es.nsi.pce.nml.jaxb.TopologyType;

/**
 *
 * @author hacksaw
 */
public interface TopologyReader {
    /**
     * Sets the remote topology endpoint.
     *
     * @param target the target to set
     */
    public void setTarget(String target);
    
    /**
     * Returns the configured remote topology endpoint.
     *
     * @return the target
     */
    public String getTarget();
    
    /**
     * 
     * @throws Exception 
     */
    public void load() throws Exception;

    /**
     * Set the last modified date of the cached remote topology document.
     *
     * @param lastModified the lastModified to set
     */
    public void setLastModified(long lastModified);
    
    /**
     * Get the date the remote topology endpoint reported as the last time the
     * NSA topology document was modified.
     *
     * @return the lastModified date of the remote topology document.
     */
    public long getLastModified();
    
    /**
     * @return the NSA managing this topology.
     */
    public NSAType getNsa();
    
    public void setNsa(NSAType nsa);
            
    /**
     * @return the topologies
     */
    public Map<String, TopologyType> getTopologies();

    /**
     * @return the ethernetPorts
     */
    public Map<String, EthernetPort> getEthernetPorts();
    
    /**
     * 
     * @return
     * @throws Exception 
     */
    public NSAType readNsaTopology() throws Exception;
}
