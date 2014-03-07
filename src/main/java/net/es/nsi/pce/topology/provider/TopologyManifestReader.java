/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

/**
 *
 * @author hacksaw
 */
public interface TopologyManifestReader {

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * topology document was modified.
     *
     * @return the lastModified date of the remote topology document.
     */
    long getLastModified();

    /**
     * Returns a current version of the master topology.  The masterTopology
     * will be loaded only if there has yet to be a successful load.
     *
     * @return Master topology.
     * @throws Exception If an error occurs when reading remote topology.
     */
    TopologyManifest getManifest() throws Exception;

    /**
     * Returns a current version of the master topology only if a new version
     * was available from the remote endpoint if available.
     *
     * @return
     * @throws Exception
     */
    TopologyManifest getManifestIfModified() throws Exception;

    /**
     * Returns the identifier of this manifest reader.
     * 
     * @return the identifier of the manifest reader.
     */
    String getId();
    
    /**
     * Returns the configured remote topology endpoint.
     *
     * @return the target
     */
    String getTarget();

    /**
     * Returns a current version of the master topology, retrieving a new
     * version from the remote endpoint if available.
     *
     * @return Master topology.
     * @throws Exception If an error occurs when reading remote topology.
     */
    void loadManifest() throws Exception;

    /**
     * Set the last modified date of the cached remote topology document.
     *
     * @param lastModified the lastModified to set
     */
    void setLastModified(long lastModified);

    /**
     * Sets the remote topology endpoint.
     *
     * @param target the target to set
     */
    void setTarget(String target);
    
}
