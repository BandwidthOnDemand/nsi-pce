package net.es.nsi.pce.api;


import javax.ws.rs.*;
import javax.ws.rs.core.Response;


@Path("/paths/")
public interface FindPathService {

    @POST
    @Path("/find/")
    @Produces( "application/json" )
    @Consumes( "application/json" )
    Response findPath(net.es.nsi.pce.api.FindPathRequest request);

}

