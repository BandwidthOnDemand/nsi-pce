package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.schema.XmlParser;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import net.es.nsi.pce.config.topo.nml.TopologyManifest;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.topology.jaxb.NetworkObject;
import net.es.nsi.pce.topology.jaxb.TopologyType;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.utils.DateUtils;

/**
 * This class reads a remote XML formatted NML topology containing the list of
 * network topologies and their NSA.  Each instance of the class
 * models a single NSA in NML.
 * 
 * @author hacksaw
 */
public class GitHubManifestReader implements TopologyManifestReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final static QName _isReference_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "isReference");
    
    // The remote location of the file to read.
    private String target = null;
    
    // Time we last read the master topology.
    private Date lastModified = new Date(0L);
    
    // The version of the last read master topology.
    private TopologyManifest manifest = null;
    
    /**
     * Default class constructor.
     */
    public GitHubManifestReader() {}
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public GitHubManifestReader(String target) {
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
    public Date getLastModified() {
        return new Date(lastModified.getTime());
    }

    /**
     * Set the last modified date of the cached remote topology document.
     * 
     * @param lastModified the lastModified to set
     */
    @Override
    public void setLastModified(Date lastModified) {
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
    private TopologyManifest readManifest() throws Exception {
        // Use the REST client to retrieve the master topology as a string.
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webGet = client.target(getTarget());
        Response response = webGet.request(MediaType.APPLICATION_XML) .header("If-Modified-Since", DateUtils.formatDate(getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        
        // A 304 Not Modified indicates we already have a up-to-date document.
        if (response.getStatus() == Status.NOT_MODIFIED.getStatusCode()) {
            return null;
        }
        
        if (response.getStatus() != Status.OK.getStatusCode()) {
            log.error("readManifest: Failed to retrieve master topology " + getTarget());
            throw new NotFoundException("Failed to retrieve master topology " + getTarget());
        }
        
        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        if (lastMod != null) {
            log.debug("readManifest: Updating last modified time " + DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123));
            setLastModified(lastMod);
        }

        // Now we want the NML XML document.
        String xml = response.readEntity(String.class);
        
        // Parse the master topology. 
        TopologyType topology = XmlParser.getInstance().parseTopologyFromString(xml);
        
        // Create an internal object to hold the master list.
        TopologyManifest newManifest = new TopologyManifest();
        newManifest.setId(topology.getId());
        newManifest.setVersion(topology.getVersion());
        
        // Pull out the indivdual network entries.
        List<NetworkObject> networkObjects = topology.getGroup();
        for (NetworkObject networkObject : networkObjects) {
            if (networkObject instanceof TopologyType) {
                TopologyType innerTopology = (TopologyType) networkObject;
                Map<QName, String> otherAttributes = innerTopology.getOtherAttributes();
                String isReference = otherAttributes.get(_isReference_QNAME);
                if (isReference != null && !isReference.isEmpty()) {
                    log.debug("readManifest: topology id: " + networkObject.getId() + ", isReference: " + isReference);
                    newManifest.setTopologyURL(networkObject.getId(), isReference);
                }
                else {
                    log.error("readManifest: topology id: " + networkObject.getId() + ", isReference: not present.");
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
        
        TopologyManifest master = this.readManifest();
        if (master != null && manifest == null) {
            // We don't have a previous version so update with this version.
            manifest = master;
        }
        else if (master != null && manifest != null) {
            // Only update if this version is newer.
            if (master.getVersion() == null || manifest.getVersion() == null) {
                // Missing version information so we have to assume an update.
                manifest = master;
            }
            else if (master.getVersion().compare(manifest.getVersion()) == DatatypeConstants.GREATER) {
                manifest = master;
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
            if (newMasterTopology.getVersion().compare(oldMasterTopology.getVersion()) == DatatypeConstants.GREATER) {
                return manifest;
            }
        }
        
        // There must not have been a change.
        return null;
    }
}
