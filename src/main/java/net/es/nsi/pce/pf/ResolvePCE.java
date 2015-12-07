package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.jaxb.topology.NetworkType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.DdsTopologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using control plane topology compute the peer NSA each of the reservation
 * segments should be sent to on route to their final destination.
 *
 * @author hacksaw
 */
public class ResolvePCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * For each path segment returned by PCE resolve the next hop NSA and
     * associated CS endpoint.
     *
     * @param pceData
     * @return
     */
    @Override
    public PCEData apply(PCEData pceData) throws WebApplicationException {
        NsiTopology topology = pceData.getTopology();
        DdsTopologyProvider tp = DdsTopologyProvider.getInstance();

        if (Strings.isNullOrEmpty(topology.getLocalNsaId())) {
            log.error("ResolvePCE: local NSA identifier is not assigned so cannot resolve control plane routes.");
            throw Exceptions.noLocalNsaIdentifier("local NSA identifier is not assigned so cannot resolve control plane routes.");
        }

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
