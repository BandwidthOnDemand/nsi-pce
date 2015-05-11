package net.es.nsi.pce.pf;


import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;

public class PolicyPCE implements PCEModule {
    @Override
    public PCEData apply(PCEData pceData) throws Exception {
        return stpPolicy(pceData);
    }

    private PCEData stpPolicy(PCEData pceData) throws Exception {
        NsiTopology nsiTopology = pceData.getTopology();

        // Parse out the constraints this PCE module supports.
        AttrConstraints constraints = new AttrConstraints(pceData.getConstraints());

        // Get source stpId.
        StringAttrConstraint sourceStp = constraints.getStringAttrConstraint(Point2PointTypes.SOURCESTP);
        if (sourceStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null"));
        }
        String srcStpId = sourceStp.getValue();

        // Get destination stpId.
        StringAttrConstraint destStp = constraints.getStringAttrConstraint(Point2PointTypes.DESTSTP);
        if (destStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null"));
        }
        String dstStpId = destStp.getValue();

        // Look up the STP within our model matching the request.
        StpType srcStp = nsiTopology.getStp(srcStpId);
        StpType dstStp = nsiTopology.getStp(dstStpId);

        // If both the source and destination STP are in the same network we
        // need to restrict the path to only that network.
        if (srcStp == null) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, Point2PointTypes.getSourceStp().getNamespace(), sourceStp.getAttrName(), sourceStp.getValue());
            throw new Exception(error);
        }
        else if (dstStp == null) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, Point2PointTypes.getDestStp().getNamespace(), destStp.getAttrName(), destStp.getValue());
            throw new Exception(error);
        }
        else if (srcStp.getNetworkId() == null) {
            String error = NsiError.getFindPathErrorString(NsiError.UNKNOWN_NETWORK, Point2PointTypes.getSourceStp().getNamespace(), sourceStp.getAttrName(), sourceStp.getValue());
            throw new Exception(error);
        }
        else if (dstStp.getNetworkId() == null) {
            String error = NsiError.getFindPathErrorString(NsiError.UNKNOWN_NETWORK, Point2PointTypes.getDestStp().getNamespace(), destStp.getAttrName(), destStp.getValue());
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
