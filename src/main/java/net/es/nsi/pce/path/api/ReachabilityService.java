package net.es.nsi.pce.path.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.context.ApplicationContext;

import com.google.gson.Gson;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ReachabilityType;
import net.es.nsi.pce.topology.jaxb.VectorType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.TopologyProvider;

@Path("/reachability")
public class ReachabilityService {

    @GET
    public Response reachability() {

        SpringContext sc  = SpringContext.getInstance();

        final ApplicationContext applicationContext = sc.getContext();
        TopologyProvider topologyProvider = (TopologyProvider) applicationContext.getBean("topologyProvider");
        final NsiTopology topology = topologyProvider.getTopology();
        final String localNetworkId = (String) applicationContext.getBean("localNetworkId");


        List<Map<String,Object>> entries = new ArrayList<>();
        for (NsaType nsa : topology.getNsas()) {
            for (ReachabilityType reachability : nsa.getReachability()) {
                if (reachability.getId().equals(localNetworkId)) {
                    for (VectorType vector : reachability.getVector()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", vector.getId());
                        entry.put("cost", vector.getCost());
                        entries.add(entry);
                    }
                }
            }
        }

        Map<String, Object> jsonHolder = new HashMap<>();

        jsonHolder.put("reachability", entries);

        Gson gson = new Gson();
        final String s = gson.toJson(jsonHolder);
        return  Response.ok().header("Content-type", "application/json").entity(s).build();
    }
}
