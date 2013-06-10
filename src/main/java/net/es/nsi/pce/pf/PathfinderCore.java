package net.es.nsi.pce.pf;


import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
import net.es.nsi.pce.config.nsa.NsaConfig;
import net.es.nsi.pce.config.nsa.NsaConfigProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.config.nsa.auth.NsaConfigAuthProvider;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.PathEndpoints;

import net.es.nsi.pce.svc.api.AuthObject;
import net.es.nsi.pce.svc.api.PathObject;
import net.es.nsi.pce.svc.api.StpObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.Map;

public class PathfinderCore {

    public ArrayList<PathObject> findPath(StpObject src, StpObject dst) throws Exception {
        ArrayList<PathObject> po = new ArrayList<PathObject>();

        ApplicationContext context = new ClassPathXmlApplicationContext("config/beans.xml");

        NsaConfigProvider ncp = (NsaConfigProvider) context.getBean("nsaConfigProvider");
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");


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

            String nsaId = ncp.getNsaId(networkId);
            NsaConfig nsaConfig = ncp.getConfigFromNetworkId(networkId);
            pathObj.nsa = nsaId;
            pathObj.providerUrl = nsaConfig.providerUrl;

            AuthObject ao = new AuthObject();
            Map<AuthCredential, String> credentials = ap.getCredentials(nsaId);
            ao.method = ap.getMethod(nsaId);

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
