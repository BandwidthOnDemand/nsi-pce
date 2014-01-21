package net.es.nsi.pce.topology.provider;

import net.es.nsi.pce.logs.PceErrors;
import net.es.nsi.pce.schema.NmlParser;
import java.util.Date;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.topology.jaxb.NmlNSAType;
import net.es.nsi.pce.logs.PceLogger;
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
    private PceLogger topologyLogger = PceLogger.getLogger();
    
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
    public NsaTopologyReader(String id, String target) {
        this.setId(id);
        this.setTarget(target);
    }
    
    /**
     * Read the NML topology from target location using HTTP GET operation.
     * 
     * @return The JAXB NSA element from the NML topology.
     */
    @Override
    public NmlNSAType readNsaTopology() throws Exception {
        // Use the REST client to retrieve the master topology as a string.
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webGet = client.target(getTarget());
        
        Response response = null;
        try {
            response = webGet.request(MediaType.APPLICATION_XML).header("If-Modified-Since", DateUtils.formatDate(new Date(getLastModified()), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            topologyLogger.error(PceErrors.AUDIT_NSA_COMMS, getTarget(), ex.getMessage());
            throw ex;
        }
        
        // A 304 Not Modified indicates we already have a up-to-date document.
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            log.debug("readNsaTopology: NOT_MODIFIED returned " + getTarget());
            return null;
        }
        
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            topologyLogger.error(PceErrors.AUDIT_NSA_COMMS, getTarget(), Integer.toString(response.getStatus()));
            throw new NotFoundException("Failed to retrieve NSA topology " + getTarget());
        }
        
        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        log.debug("readNsaTopology: lastModified = " + new Date(getLastModified()) + ", current = " + lastMod);
        
        if (lastMod != null) {
            log.debug("readNsaTopology: Updating last modified time to " + lastMod);
            setLastModified(lastMod.getTime());
        }

        // Now we want the NML XML document.  We have to read this as a string
        // because GitHub is returning incorrect media type (text/plain).
        String xml = response.readEntity(String.class);
        
        log.debug("readNsaTopology: input message " + xml);
        
        // Parse the NSA topology. 
        NmlNSAType topology = NmlParser.getInstance().parseNsaFromString(xml);
        
        // We should never get this - an exception should be thrown.
        if (topology == null) {
            topologyLogger.error(PceErrors.AUDIT_NSA_XML_PARSE, getTarget());
        }
        
        return topology;
    }
}
