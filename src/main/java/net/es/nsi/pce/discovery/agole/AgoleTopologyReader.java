package net.es.nsi.pce.discovery.agole;

import net.es.nsi.pce.management.logs.PceErrors;
import java.util.Date;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.discovery.jaxb.NmlNSAType;
import net.es.nsi.pce.discovery.provider.DiscoveryParser;
import net.es.nsi.pce.management.logs.PceLogger;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reads a remote XML formatted NML topology and creates simple
 * network objects used to later build NSI topology.  Each instance of the class
 * models a single NSA in NML.
 * 
 * @author hacksaw
 */
public class AgoleTopologyReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private PceLogger topologyLogger = PceLogger.getLogger();

    private String id;
    private String target;
    private long lastModifiedTime;

    private ClientConfig clientConfig;
    
    /**
     * Default class constructor.
     */
    public AgoleTopologyReader() {
        clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
    }
    
    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public AgoleTopologyReader(String id, String target, long lastModifiedTime) {
        this.id = id;
        this.target = target;
        this.lastModifiedTime = lastModifiedTime;
        clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
    }
    
    /**
     * Read the NML topology from target location using HTTP GET operation.
     * 
     * @return The JAXB NSA element from the NML topology.
     */
    public NmlNSAType readNsaTopology() throws Exception {
        // Use the REST client to retrieve the master topology as a string.
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webGet = client.target(target);
        
        Response response = null;
        try {
            response = webGet.request(MediaType.APPLICATION_XML).header("If-Modified-Since", DateUtils.formatDate(new Date(getLastModifiedTime()), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            topologyLogger.errorAudit(PceErrors.AUDIT_NSA_COMMS, target, ex.getMessage());
            client.close();
            throw ex;
        }
        
        // A 304 Not Modified indicates we already have a up-to-date document.
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            log.debug("readNsaTopology: NOT_MODIFIED returned " + target);
            client.close();
            response.close();
            return null;
        }
        
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            topologyLogger.errorAudit(PceErrors.AUDIT_NSA_COMMS, target, Integer.toString(response.getStatus()));
            client.close();
            response.close();
            throw new NotFoundException("Failed to retrieve NSA topology " + target);
        }
        
        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        log.debug("readNsaTopology: lastModified = " + new Date(getLastModifiedTime()) + ", current = " + lastMod);
        
        if (lastMod != null) {
            log.debug("readNsaTopology: Updating last modified time to " + lastMod);
            lastModifiedTime = lastMod.getTime();
        }

        // Now we want the NML XML document.  We have to read this as a string
        // because GitHub is returning incorrect media type (text/plain).
        String xml = response.readEntity(String.class);
        client.close();
        response.close();
 
        log.debug("readNsaTopology: input message " + xml);
        
        // Parse the NSA topology. 
        NmlNSAType topology = DiscoveryParser.getInstance().parseNsaFromString(xml);
        
        // We should never get this - an exception should be thrown.
        if (topology == null) {
            topologyLogger.errorAudit(PceErrors.AUDIT_NSA_XML_PARSE, target);
        }
        
        return topology;
    }

    /**
     * @return the lastModifiedTime
     */
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
}
