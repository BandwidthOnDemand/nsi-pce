package net.es.nsi.pce.pf;


import net.es.nsi.pce.config.SpringContext;

import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;

import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.svc.api.AuthObject;
import net.es.nsi.pce.svc.api.FindPathAlgorithm;
import net.es.nsi.pce.svc.api.PathObject;
import net.es.nsi.pce.svc.api.StpObject;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Map;

public class PathfinderCore {


    public ArrayList<PathObject> findPath(StpObject src, StpObject dst, FindPathAlgorithm algorithm) throws Exception {
        ArrayList<PathObject> po = new ArrayList<PathObject>();
        SpringContext sc  = SpringContext.getInstance();
        ApplicationContext context = sc.getContext();

        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");
        TopologyProvider tp = (TopologyProvider) context.getBean("topologyProvider");
        tp.loadTopology();

        PCEData pceData = new PCEData();

        TopoPathEndpoints pe = new TopoPathEndpoints();
        pe.setSrcLocal(src.localId);
        pe.setSrcNetwork(src.networkId);

        pe.setDstLocal(dst.localId);
        pe.setDstNetwork(dst.networkId);
        pceData.getConstraints().add(pe);
        pceData.setTopo(tp.getTopology());

        PCEModule pce;
        if (algorithm == null) {
            pce = (PCEModule) context.getBean("chainPCE");

        } else if (algorithm.equals(FindPathAlgorithm.CHAIN)) {
            pce = (PCEModule) context.getBean("chainPCE");

        } else if (algorithm.equals(FindPathAlgorithm.TREE)) {
            pce = (PCEModule) context.getBean("treePCE");
        } else {
            pce = (PCEModule) context.getBean("chainPCE");
        }

        PCEData result = pce.apply(pceData);

        if (result == null) {
            throw new Exception("null result");
        } else if (result.getPath() == null) {

            throw new Exception("null path");
        }

        for (StpPair stpPair: result.getPath().getStpPairs() ) {
            String networkId = stpPair.getA().getNetwork().getNetworkId();

            PathObject pathObj = new PathObject();
            StpObject aStpObj = new StpObject();
            aStpObj.localId = stpPair.getA().getLocalId();
            aStpObj.networkId = networkId;
            StpObject zStpObj = new StpObject();
            zStpObj.localId = stpPair.getZ().getLocalId();
            zStpObj.networkId = networkId;

            pathObj.sourceStp = aStpObj;
            pathObj.destinationStp = zStpObj;

            ServiceInfo si = sip.byNetworkId(networkId);

            pathObj.nsa = si.getNsaId();
            pathObj.providerUrl = si.getProviderUrl();


            AuthObject ao = new AuthObject();
            Map<AuthCredential, String> credentials = ap.getCredentials(si.getNsaId());
            ao.method = ap.getMethod(si.getNsaId());

            if (credentials.containsKey(AuthCredential.TOKEN)) {
                ao.token = credentials.get(AuthCredential.TOKEN);
            }
            if (credentials.containsKey(AuthCredential.USERNAME)) {
                ao.username = credentials.get(AuthCredential.USERNAME);
            }

            if (credentials.containsKey(AuthCredential.PASSWORD)) {
                ao.password = credentials.get(AuthCredential.PASSWORD);
            }

            pathObj.auth = ao;

            po.add(pathObj);

        }





        return po;
    }
}
