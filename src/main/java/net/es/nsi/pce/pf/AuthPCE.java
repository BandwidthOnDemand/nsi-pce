package net.es.nsi.pce.pf;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.topo.Topology;
import net.es.nsi.pce.svc.api.AuthMethod;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class AuthPCE implements PCEModule {
    public PCEData apply(PCEData pceData) throws Exception {
        System.out.println("authPCE start");
        SpringContext sc  = SpringContext.getInstance();
        ApplicationContext context = sc.getContext();
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");


        Topology topo = pceData.getTopo();
        Topology newTopo = new Topology();
        for (String networkId : topo.getNetworkIds()) {
            ServiceInfo si = sip.byNetworkId(networkId);
            if (si == null) {
                System.err.println("could not find service info for network id "+networkId+", not including it in topology");
            } else {
                String nsaId = si.getNsaId();
                System.out.println(" net: "+networkId + " -- nsa: "+ nsaId);
                AuthMethod method = ap.getMethod(nsaId);
                Map<AuthCredential, String> credentials = ap.getCredentials(nsaId);

                if (method == null) {
                    System.out.println("no auth method known for network "+networkId+", not including it in topology");

                } else if (method.equals(AuthMethod.NONE)) {
                    newTopo.setNetwork(networkId, topo.getNetwork(networkId));
                } else {
                    if (credentials == null || credentials.isEmpty()) {
                        System.out.println("no credentials for network "+networkId+", not including it in topology");
                    } else {
                        newTopo.setNetwork(networkId, topo.getNetwork(networkId));
                    }
                }

            }
        }


        PCEData result = pceData;
        result.setTopo(newTopo);
        return result;


    }
}
