package net.es.nsi.pce.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;


@Path("/")

public interface AggService {


    @POST
    @Path("/pathreply/")
    @Produces( "application/json" )
    @Consumes( "application/json" )
    Response pathReply(net.es.nsi.pce.api.FindPathResponse response);

}
