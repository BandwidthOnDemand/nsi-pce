package net.es.nsi.pce.pf;


import com.google.common.base.Optional;
import java.util.List;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.jaxb.OrderedStpType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.jaxb.StpListType;
import net.es.nsi.pce.path.services.Service;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PolicyPCE performs topology filtering based on defined policies.
 *
 * @author hacksaw
 */
public class PolicyPCE implements PCEModule {
    private static final Logger log = LoggerFactory.getLogger(PolicyPCE.class);

    /**
     * Apply policy to the provided topology based on reservation input
     * parameters.
     *
     * @param pceData
     * @return
     * @throws Exception
     */
    @Override
    public PCEData apply(PCEData pceData) throws Exception {
        // Get constraints this PCE module supports.
        AttrConstraints constraints = new AttrConstraints(pceData.getConstraints());

        // We currently implement P2PS policies in the PCE.
        String serviceType = PfUtils.getServiceTypeOrFail(constraints);
        List<Service> serviceByType = Service.getServiceByType(serviceType);
        if (serviceByType.contains(Service.P2PS)) {
            return stpPolicy(constraints, pceData);
        }

        // If we have no policies to apply then fall through.
        return pceData;
    }

    /**
     * Implements basic STP policies that apply to all reservations.
     *
     * @param pceData
     * @return
     * @throws Exception
     */
    private PCEData stpPolicy(AttrConstraints constraints, PCEData pceData) throws Exception {
        // Get the network topology.
        NsiTopology nsiTopology = pceData.getTopology();

        // Get the P2P service parameters.
        P2PServiceBaseType p2p = PfUtils.getP2PServiceBaseTypeOrFail(constraints);

        // Get source and destionation stpId.
        String srcStpId = PfUtils.getSourceStpOrFail(p2p);
        String dstStpId = PfUtils.getDestinationStpOrFail(p2p);

        SimpleStp srcStp = new SimpleStp(PfUtils.getSourceStpOrFail(p2p));
        SimpleStp dstStp = new SimpleStp(PfUtils.getDestinationStpOrFail(p2p));

        // Make sure these STP are from known networks.
        Optional<NetworkType> srcNetwork = Optional.fromNullable(nsiTopology.getNetworkById(srcStp.getNetworkId()));
        if (!srcNetwork.isPresent()) {
            log.error("stpPolicy: source STP has unknown networkId: " + srcStp.getId());
            throw Exceptions.stpUnknownNetwork(srcStpId);
        }

        Optional<NetworkType> dstNetwork = Optional.fromNullable(nsiTopology.getNetworkById(dstStp.getNetworkId()));
        if (!dstNetwork.isPresent()) {
            log.error("stpPolicy: destination STP has unknown networkId: " + dstStp.getId());
            throw Exceptions.stpUnknownNetwork(dstStpId);
        }

        // Validate all members of the ERO are also of known networks.
        Optional<StpListType> ero = Optional.fromNullable(p2p.getEro());
        if (ero.isPresent()) {
            for (OrderedStpType stp : ero.get().getOrderedSTP()) {
                try {
                    String networkId = SimpleStp.parseNetworkId(stp.getStp());
                    Optional<NetworkType> network = Optional.fromNullable(nsiTopology.getNetworkById(networkId));
                    if (!network.isPresent()) {
                        log.error("stpPolicy: ERO contains STP without unknown networkId: " + stp.getStp());
                        throw Exceptions.stpUnknownNetwork(stp.getStp());
                    }
                }
                catch (IllegalArgumentException ex) {
                    log.error("stpPolicy: ERO contains STP without networkId: " + stp.getStp(), ex);
                    throw Exceptions.stpUnknownNetwork(stp.getStp());
                }
            }
        }

        // Now we restrict routing of two enpoints in the same domain to only
        // route within that domain, otherwise use the full topology.
        if (srcNetwork.get().getId().equalsIgnoreCase(dstNetwork.get().getId())) {
            // Build a new topology containing only elements from this network.
            NsiTopology tp = nsiTopology.getTopologyByNetworkId(srcStp.getNetworkId());
            pceData.setTopology(tp);
            return pceData;
        }

        return pceData;
    }
}
