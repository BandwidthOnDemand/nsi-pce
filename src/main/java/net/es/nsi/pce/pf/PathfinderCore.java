package net.es.nsi.pce.pf;


import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.PathEndpoints;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.svc.api.AuthMethod;
import net.es.nsi.pce.svc.api.AuthObject;
import net.es.nsi.pce.svc.api.PathObject;
import net.es.nsi.pce.svc.api.StpObject;

import java.util.ArrayList;

public class PathfinderCore {

    public ArrayList<PathObject> findPath(StpObject src, StpObject dst) throws Exception {
        ArrayList<PathObject> po = new ArrayList<PathObject>();

        AuthObject ao = new AuthObject();
        ao.method = AuthMethod.NONE;
        PCEData pceData = new PCEData();

        PathEndpoints pe = new PathEndpoints();
        pe.setSrcLocal(src.localId);
        pe.setSrcNetwork(src.networkId);
        pe.setDstLocal(dst.localId);
        pe.setDstNetwork(dst.networkId);
        pceData.getConstraints().add(pe);


        PretendPCE pretend = new PretendPCE();
        PCEData result = pretend.apply(pceData);

        for (StpPair stpPair: result.getPath().getStpPairs() ) {
            PathObject pathObj = new PathObject();
            StpObject aStpObj = new StpObject();
            aStpObj.localId = stpPair.getA().localId;
            aStpObj.networkId = stpPair.getA().network.getNetworkId();
            StpObject zStpObj = new StpObject();
            zStpObj.localId = stpPair.getZ().localId;
            zStpObj.networkId = stpPair.getZ().network.getNetworkId();

            pathObj.sourceStp = aStpObj;
            pathObj.destinationStp = zStpObj;

            // TODO fix those
            pathObj.nsa = stpPair.getA().network.getNetworkId();
            pathObj.providerUrl = src.networkId;
            pathObj.auth = ao;
            po.add(pathObj);

        }





        return po;
    }
}
