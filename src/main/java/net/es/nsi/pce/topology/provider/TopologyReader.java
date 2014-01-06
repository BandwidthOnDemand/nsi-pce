/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import net.es.nsi.pce.topology.jaxb.NmlNSAType;
import net.es.nsi.pce.topology.model.NsiTopology;

/**
 *
 * @author hacksaw
 */
public interface TopologyReader {
    /**
     * Returns the identifier for this topology.
     * 
     * @return the target
     */
    public String getId();

    /**
     * @param id the id to set
     */
    public void setId(String id);
    
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
    public NmlNSAType getNsa();
    
    public void setNsa(NmlNSAType nsa);
    
    public NsiTopology getNsiTopology();
    
    /**
     * 
     * @return
     * @throws Exception 
     */
    public NmlNSAType readNsaTopology() throws Exception;
}
