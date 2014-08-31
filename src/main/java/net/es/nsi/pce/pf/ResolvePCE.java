package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.DdsTopologyProvider;

/**
 * Using control plane topology compute the peer NSA each of the reservation
 * segments should be sent to on route to their final destination.
 * 
 * @author hacksaw
 */
public class ResolvePCE implements PCEModule {
    @Override
    public PCEData apply(PCEData pceData) {
        NsiTopology topology = pceData.getTopology();
        DdsTopologyProvider tp = DdsTopologyProvider.getInstance();

        for (PathSegment segment : pceData.getPath().getPathSegments()) {
            // Find the NSA managing this segment.
            String networkId = segment.getStpPair().getA().getNetworkId();
            NetworkType network = topology.getNetworkById(networkId);

            // Find a path to this NSA through the control plane.
            String nextNsa = tp.getControlPlaneTopology().findNextNsa(topology.getLocalNsaId(), network.getNsa().getId());
            segment.setNsaId(nextNsa);
            Optional<String> providerUrl = topology.getProviderUrl(nextNsa);
            if (providerUrl.isPresent()) {
              segment.setCsProviderURL(providerUrl.get());
            }
        }

        return pceData;
    }
}
