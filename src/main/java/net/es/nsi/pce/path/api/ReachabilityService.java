package net.es.nsi.pce.path.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/reachability")
public class ReachabilityService {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response reachability() {

        // simply return it as json object (the reachability table is a Map<String, Map<String, Integer>> so that should auto-marshal)
        return Response.ok().build();
    }
}
