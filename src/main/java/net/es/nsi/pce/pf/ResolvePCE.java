package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaInterfaceType;
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
            for (NsaInterfaceType anInteface : nsa.getInterface()) {
                if (NsiConstants.NSI_CS_PROVIDER_V2.equalsIgnoreCase(anInteface.getType())) {
                    segment.setCsProviderURL(anInteface.getHref().trim());
                    break;
                }
            }
        }

        return pceData;
    }
}
