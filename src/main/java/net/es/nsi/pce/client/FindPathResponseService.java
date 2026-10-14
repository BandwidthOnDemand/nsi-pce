package net.es.nsi.pce.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.es.nsi.pce.jaxb.path.FindPathResponseType;
import net.es.nsi.pce.jaxb.path.FindPathStatusType;

@Path("/aggregator/")
public class FindPathResponseService {

    @POST
    @Path("/path")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response findPathResponse(FindPathResponseType response) {
        System.out.println("findPathResponse: " + response.getCorrelationId() + ", " + response.getStatus().name());

        if (response.getStatus() == FindPathStatusType.FAILED) {
            System.out.println("findPathResponse: error code=" + response.getFindPathError().getCode() + ", description=" + response.getFindPathError().getDescription());
        }

        try {
            TestServer.INSTANCE.setFindPathResponse(response);
        }
        catch (Exception ex) {
            System.err.println("findPathResponse: exception " + ex.getLocalizedMessage());
        }
        return Response.ok().build();
    }

}