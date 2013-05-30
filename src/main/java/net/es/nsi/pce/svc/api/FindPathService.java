package net.es.nsi.pce.svc.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/paths/")
public interface FindPathService {

    @POST
    @Path("/find/")
    @Produces( "application/json" )
    @Consumes( "application/json" )
    Response findPath(FindPathRequest request);

}