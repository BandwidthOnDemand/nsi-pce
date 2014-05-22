package net.es.nsi.pce.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.path.jaxb.FindPathResponseType;

@Path("/aggregator/")
public class FindPathResponseService {

    @POST
    @Path("/path")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response findPathResponse(FindPathResponseType response) {
        System.out.println("findPathResponse: " + response.getCorrelationId() + ", " + response.getStatus().name());

        try {
            TestServer.INSTANCE.setFindPathResponse(response);
        }
        catch (Exception ex) {
            System.err.println("findPathResponse: exception " + ex.getLocalizedMessage());
        }
        return Response.ok().build();
    }

}