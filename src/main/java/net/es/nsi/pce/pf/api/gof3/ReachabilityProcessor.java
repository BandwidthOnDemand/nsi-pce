package net.es.nsi.pce.pf.api.gof3;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
public class ReachabilityProcessor {

    @Autowired
    private TopologyProvider topologyProvider;


    public List<VectorType> getCurrentReachabilityInfo(){
        List<VectorType> result = new ArrayList<>();

        NsiTopology nsiTopology = topologyProvider.getTopology();
        // put our own networks in with cost 0
        List<String> localNetworkIds = nsiTopology.getLocalNetworks();
        for (String localNetworkId: localNetworkIds){
            VectorType localVector = new VectorType();
            localVector.setId(localNetworkId);
            localVector.setCost(0);
            result.add(localVector);
        }

        for (NsaType nsa : nsiTopology.getNsas()) {
            for (ReachabilityType reachability : nsa.getReachability()) {
                for (VectorType vectorType: reachability.getVector()) {
                    if (!localNetworkIds.contains(vectorType.getId())) {  // so we don't re-advertise our own networks
                        VectorType resultVector = new VectorType();
                        resultVector.setId(vectorType.getId());
                        resultVector.setCost(vectorType.getCost() + 1);
                        result.add(resultVector);
                    }

                }

            }
        }
        return result;
    }




}
