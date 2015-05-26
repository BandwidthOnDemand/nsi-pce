package net.es.nsi.pce.pf;


import com.google.common.base.Strings;
import java.util.List;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.path.services.Service;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;

/**
 * The PolicyPCE performs topology filtering based on defined policies.
 *
 * @author hacksaw
 */
public class PolicyPCE implements PCEModule {
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
        StringAttrConstraint serviceType = constraints.getStringAttrConstraint(PCEConstraints.SERVICETYPE);
        List<Service> serviceByType = Service.getServiceByType(serviceType.getValue());
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
        NsiTopology nsiTopology = pceData.getTopology();

        // Get the P2P service parameters.
        ObjectAttrConstraint p2pObject = constraints.getObjectAttrConstraint(Point2PointTypes.P2PS);
        if (p2pObject == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.P2PS, "null", "null"));
        }
        P2PServiceBaseType p2p = p2pObject.getValue(P2PServiceBaseType.class);

        // Get source stpId.
        String srcStpId = p2p.getSourceSTP();
        if (Strings.isNullOrEmpty(srcStpId)) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null"));
        }

        // Get destination stpId.
        String dstStpId = p2p.getDestSTP();
        if (Strings.isNullOrEmpty(dstStpId)) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null"));
        }

        // Look up the STP within our model matching the request.
        StpType srcStp = nsiTopology.getStp(srcStpId);
        StpType dstStp = nsiTopology.getStp(dstStpId);

        // If both the source and destination STP are in the same network we
        // need to restrict the path to only that network.
        if (srcStp == null) {
            String error = NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), srcStpId);
            throw new Exception(error);
        }
        else if (dstStp == null) {
            String error = NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), dstStpId);
            throw new Exception(error);
        }
        else if (srcStp.getNetworkId() == null) {
            String error = NsiError.getFindPathErrorString(NsiError.UNKNOWN_NETWORK, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), srcStpId);
            throw new Exception(error);
        }
        else if (dstStp.getNetworkId() == null) {
            String error = NsiError.getFindPathErrorString(NsiError.UNKNOWN_NETWORK, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), dstStpId);
            throw new Exception(error);
        }
        else if (!srcStp.getNetworkId().equalsIgnoreCase(dstStp.getNetworkId())) {
            return pceData;
        }

        // Build a new topology containing only elements from this network.
        NsiTopology tp = nsiTopology.getTopologyByNetworkId(srcStp.getNetworkId());
        pceData.setTopology(tp);
        return pceData;
    }
}
