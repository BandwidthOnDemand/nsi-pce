package net.es.nsi.pce.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.api.jaxb.FindPathResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/aggregator/")
public class FindPathResponseServiceImpl implements FindPathResponseService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @POST
    @Path("/path/")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Override
    public Response findPathResponse(FindPathResponseType response) {
        System.out.println("findPathResponse: " + response.getCorrelationId() + ", " + response.getStatus().name());
        return Response.ok().build();
    }

}