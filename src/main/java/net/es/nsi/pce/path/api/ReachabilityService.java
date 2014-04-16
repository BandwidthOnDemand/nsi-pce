package net.es.nsi.pce.path.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.context.ApplicationContext;

import com.google.gson.Gson;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.pf.api.gof3.ReachabilityProcessor;

@Path("/reachability")
// Todo make this bean spring-managed and thus eliminate the calls to the ApplicationContext
public class ReachabilityService {

    @GET
    public Response reachability() {
        SpringContext sc = SpringContext.getInstance();
        final ApplicationContext applicationContext = sc.getContext();
        ReachabilityProcessor reachabilityProcessor = (ReachabilityProcessor) applicationContext.getBean("reachabilityProcessor");

        final Map<String, Integer> currentReachabilityInfo = reachabilityProcessor.getCurrentReachabilityInfo();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String id: currentReachabilityInfo.keySet()){
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", id);
            entry.put("cost", currentReachabilityInfo.get(id));
            result.add(entry);
        }

        Map<String, Object> jsonHolder = new HashMap<>();
        jsonHolder.put("reachability", result);
        Gson gson = new Gson();
        final String s = gson.toJson(jsonHolder);
        return  Response.ok().header("Content-type", "application/json").entity(s).build();
    }
}
