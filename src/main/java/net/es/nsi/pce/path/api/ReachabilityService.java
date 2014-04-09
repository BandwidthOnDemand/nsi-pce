package net.es.nsi.pce.path.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

@Path("/reachability")
public class ReachabilityService {

    @GET
    public Response reachability() {

        Map<String, Object> holder = new HashMap<>();

        Map<String, Object> entry = new HashMap<>();
        entry.put("id", "http://some.topology");
        entry.put("cost", new Integer(3));

        // simply return it as json object (the reachability table is a Map<String, Map<String, Integer>> so that should auto-marshal)
        holder.put("reachability", Arrays.asList(entry));

        // marshall the simple way as Jersey mystifies me
        Gson gson = new Gson();
        final String s = gson.toJson(holder);
        return  Response.ok().header("Content-type", "application/json").entity(s).build();
    }
}
