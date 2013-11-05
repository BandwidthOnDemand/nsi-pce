package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.schema.XmlParser;
import java.util.Date;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.topology.jaxb.NSAType;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class reads a remote XML formatted NML topology and creates simple
 * network objects used to later build NSI topology.  Each instance of the class
 * models a single NSA in NML.
 * 
 * @author hacksaw
 */
@Component
public class NsaTopologyReader extends NmlTopologyReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * Default class constructor.
     */
    public NsaTopologyReader() {}
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public NsaTopologyReader(String target) {
        this.setTarget(target);
    }
    
    /**
     * Read the NML topology from target location using HTTP GET operation.
     * 
     * @return The JAXB NSA element from the NML topology.
     */
    @Override
    public NSAType readNsaTopology() throws Exception {
        log.debug("readNsaTopology: entering");
        
        // Use the REST client to retrieve the master topology as a string.
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webGet = client.target(getTarget());
        Response response = webGet.request(MediaType.APPLICATION_XML) .header("If-Modified-Since", DateUtils.formatDate(new Date(getLastModified()), DateUtils.PATTERN_RFC1123)).get();
        
        // A 304 Not Modified indicates we already have a up-to-date document.
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            log.debug("readNsaTopology: NOT_MODIFIED returned " + getTarget());
            return null;
        }
        
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.error("readNsaTopology: Failed to retrieve NSA topology " + getTarget());
            throw new NotFoundException("Failed to retrieve NSA topology " + getTarget());
        }
        
        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        log.debug("readNsaTopology: lastModified = " + new Date(getLastModified()) + ", current = " + lastMod);
        
        if (lastMod != null) {
            log.debug("readNsaTopology: Updating last modified time to " + lastMod);
            setLastModified(lastMod.getTime());
        }

        // Now we want the NML XML document.
        String xml = response.readEntity(String.class);
        
        log.debug("readNsaTopology: input message " + xml);
        
        // Parse the NSA topology. 
        NSAType topology = XmlParser.getInstance().parseNsaFromString(xml);
        
        // We shoudl never get this - an exception should be thrown.
        if (topology == null) {
            log.error("readNsaTopology: Failed to parse NSA topology " + getTarget());
        }
        
        return topology;
    }
}
