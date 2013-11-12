/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/topology")
public class TopologyService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Factory for FindPath request and response messages.
    private final static ObjectFactory factory = new ObjectFactory();
    
    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response ping() throws Exception {
        log.debug("ping: Topology PING!");
        return Response.ok().build();
    }
}
