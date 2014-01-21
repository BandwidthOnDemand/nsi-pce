package net.es.nsi.pce.topology.provider;

import net.es.nsi.pce.logs.PceErrors;
import java.io.File;
import net.es.nsi.pce.config.topo.nml.TopologyManifest;
import net.es.nsi.pce.logs.PceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class reads a remote XML formatted NML topology containing the list of
 * network topologies and their NSA.  Each instance of the class
 * models a single NSA in NML.
 * 
 * @author hacksaw
 */
@Component
public class FileManifestReader implements TopologyManifestReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private PceLogger topologyLogger = PceLogger.getLogger();
    
    // The manifest identifier.
    private String id = getClass().getName();
    
    // The remote location of the file to read.
    private String target = null;
    
    // Time we last read the master topology.
    private long lastModified = 0;
    
    // The version of the last read master topology.
    private TopologyManifest manifest = null;
    
    /**
     * Default class constructor.
     */
    public FileManifestReader() {}
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public FileManifestReader(String target) {
        this.target = target;
    }

    /**
     * Returns the configured remote topology endpoint.
     * 
     * @return the target
     */
    @Override
    public String getTarget() {
        return target;
    }

    /**
     * Sets the remote topology endpoint.
     * 
     * @param target the target to set
     */
    @Override
    public void setTarget(String target) {
        this.target = target;
    }
    
    /**
     * Get the date the remote topology endpoint reported as the last time the
     * topology document was modified.
     * 
     * @return the lastModified date of the remote topology document.
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Set the last modified date of the cached remote topology document.
     * 
     * @param lastModified the lastModified to set
     */
    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * Read the NML topology from target location using HTTP GET operation.
     * This method will return a list of target topology endpoints if the
     * master topology document was retrieved.  NULL is returned if the
     * topology endpoint reports no modifications since last retrieval.  An
     * exception is thrown for any errors.
     * 
     * @return The list of topology endpoints from the remote NML topology.
     */
    private TopologyManifest readManifest() throws NullPointerException {
        // Get a list of files for supplied directory.
        File folder = null;
        try {
            folder = new File(target);
        }
        catch (NullPointerException ex) {
            topologyLogger.error(PceErrors.AUDIT_MANIFEST_FILE, "FileManifestReader", target);
            throw ex;
        }
        
        long lastMod = folder.lastModified();
        
        // If directory was not modified we return.
        if (lastMod <= lastModified) {
            return null;
        }
        
        // Save the new modify time.
        setLastModified(lastMod);
        
        // We will grab all XML files from the target directory. 
        File[] listOfFiles = folder.listFiles(); 

        log.info("Loading topology information from directory " + folder.getAbsolutePath());
        
        // We populate the list of files in a TopologyManifest object.
        TopologyManifest newManifest = new TopologyManifest();
        newManifest.setId(folder.getAbsolutePath());
        newManifest.setVersion(lastMod);
        
        String file;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                file = listOfFiles[i].getAbsolutePath();
                if (file.endsWith(".xml") || file.endsWith(".xml")) {
                    newManifest.put(file, file);
                }
            }
        }

        return newManifest;
    }

    /**
     * Returns a current version of the master topology, retrieving a new
     * version from the remote endpoint if available.
     * 
     * @return Master topology.
     * @throws Exception If an error occurs when reading remote topology.
     */
    @Override
    public synchronized void loadManifest() throws Exception {
        
        TopologyManifest newManifest = this.readManifest();
        if (newManifest != null && manifest == null) {
            // We don't have a previous version so update with this version.
            manifest = newManifest;
        }
        else if (newManifest != null && manifest != null) {
            // Only update if this version is newer.
            if (newManifest.getVersion() == 0) {
                // Missing version information so we have to assume an update.
                manifest = newManifest;
            }
            else if (newManifest.getVersion() > manifest.getVersion()) {
                manifest = newManifest;
            }
        }
    }
    
    /**
     * Returns a current version of the master topology.  The masterTopology
     * will be loaded only if there has yet to be a successful load.
     * 
     * @return Master topology.
     * @throws Exception If an error occurs when reading remote topology.
     */
    @Override
    public TopologyManifest getManifest() throws Exception {
        if (manifest == null) {
            loadManifest();
        }
        
        return manifest;
    }
    
    /**
     * Returns a current version of the master topology only if a new version
     * was available from the remote endpoint if available.
     * 
     * @return
     * @throws Exception 
     */
    @Override
    public TopologyManifest getManifestIfModified() throws Exception {
        TopologyManifest oldMasterTopology = manifest;
        loadManifest();
        TopologyManifest newMasterTopology = manifest;

        if (newMasterTopology != null && oldMasterTopology == null) {
            // We don't have a previous version so there is a change.
            return manifest;
        }
        else if (newMasterTopology != null && oldMasterTopology != null) {
            // Only update if this version is newer.
            if (newMasterTopology.getVersion() > oldMasterTopology.getVersion()) {
                return manifest;
            }
        }
        
        // There must not have been a change.
        return null;
    }

    /**
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }
}
