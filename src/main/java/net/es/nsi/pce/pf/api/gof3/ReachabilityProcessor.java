package net.es.nsi.pce.pf.api.gof3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ReachabilityType;
import net.es.nsi.pce.topology.jaxb.VectorType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.TopologyProvider;

/**
* Reads the raw reachability data, and then does the following:
 * - adds an entry for our own network with cost=0
 * - increments the reachability info provided by our peers with 1
*/
public class ReachabilityProcessor {

    private TopologyProvider topologyProvider;

    ReachabilityProcessor(TopologyProvider topologyProvider) {
        this.topologyProvider = topologyProvider;
    }

    /**
     *
     * @return a map where the key is the network id and the value is the cost
     */
    public Map<String,Integer> getCurrentReachabilityInfo(){
        Map<String, Integer> result = new HashMap<>();

        NsiTopology nsiTopology = topologyProvider.getTopology();
        // put our own networks in with cost 1
        List<String> localNetworkIds = nsiTopology.getLocalNetworks();
        for (String localNetworkId: localNetworkIds){
            result.put(localNetworkId, 1);
        }

        return makeResult(new ArrayList<>(nsiTopology.getNsas()), localNetworkIds, result);
    }

    @VisibleForTesting
    protected Map<String, Integer> makeResult(List<NsaType> nsas, List<String> localNetworkIds, Map<String,Integer> intermediateResult){
        for (NsaType nsa : nsas) {
            for (ReachabilityType reachability : nsa.getReachability()) {
                for (final VectorType vectorType: reachability.getVector()) {
                    if (!localNetworkIds.contains(vectorType.getId())) {  //don't re-advertise our own networks
                        String networkId = vectorType.getId();
                        Integer cost = vectorType.getCost() + 1;
                        if (!intermediateResult.containsKey(networkId) || intermediateResult.get(networkId) > cost) {
                            // make sure we don't advertise a route that is more expensive
                            intermediateResult.put(networkId, cost);
                        }
                    }
                }

            }
        }
        return intermediateResult;
    }

}
