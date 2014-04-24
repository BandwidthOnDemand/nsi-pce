package net.es.nsi.pce.pf;

import com.google.common.base.Optional;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.model.NsiTopology;


/**
 *
 * @author hacksaw
 */
public class ResolvePCE implements PCEModule {

    @Override
    public PCEData apply(PCEData pceData) {
        NsiTopology topology = pceData.getTopology();

        for (PathSegment segment : pceData.getPath().getPathSegments()) {
            String networkId = segment.getStpPair().getA().getNetworkId();
            NetworkType network = topology.getNetworkById(networkId);
            NsaType nsa = topology.getNsa(network.getNsa().getId());
            segment.setNsaId(nsa.getId());

            Optional<String> providerUrl = topology.getProviderUrl(nsa.getId());

            if (providerUrl.isPresent()) {
              segment.setCsProviderURL(providerUrl.get());
            }
        }

        return pceData;
    }
}
