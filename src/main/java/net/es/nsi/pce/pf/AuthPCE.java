package net.es.nsi.pce.pf;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.topo.Topology;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import net.es.nsi.pce.api.jaxb.AuthMethodType;
import net.es.nsi.pce.pf.api.topo.Sdp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Authentication and Authorization Path Computation module.  At the moment
 * this module looks up NSA credentials relating to the networks involved in
 * the path result.
 * 
 * @author hacksaw
 */
public class AuthPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public PCEData apply(PCEData pceData) throws Exception {
        log.debug("AuthPCE.apply: Starting ...");
        
        SpringContext sc  = SpringContext.getInstance();
        ApplicationContext context = sc.getContext();
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");

        Topology topo = pceData.getTopology();
        Topology newTopo = new Topology();
        for (String networkId : topo.getNetworkIds()) {
            ServiceInfo si = sip.byNetworkId(networkId);
            if (si == null) {
                log.error("AuthPCE.apply: Could not find service info for network id " + networkId + ", not including it in topology.");
            } else {
                String nsaId = si.getNsaId();
                
                log.debug("AuthPCE.apply: networkId = " + networkId + ", nsaId = " + nsaId);
                
                AuthMethodType method = ap.getMethod(nsaId);
                Map<AuthCredential, String> credentials = ap.getCredentials(nsaId);

                if (method == null) {
                    log.error("AuthPCE.apply: No auth method known for networkId " + networkId + ", not including it in topology.");

                } else if (method.equals(AuthMethodType.NONE)) {
                    newTopo.addNetwork(topo.getNetworkById(networkId));
                } else {
                    if (credentials == null || credentials.isEmpty()) {
                        log.error("AuthPCE.apply: No credentials for networkId " + networkId + ", not including it in topology");
                    } else {
                        newTopo.addNetwork(topo.getNetworkById(networkId));
                    }
                }
            }
        }

        // Now we prune bidirectional SDP from the list that have had their networks removed.
        for (Sdp sdp : topo.getSdps()) {
            if (newTopo.getNetworkById(sdp.getA().getNetwork().getNetworkId()) != null &&
                    newTopo.getNetworkById(sdp.getZ().getNetwork().getNetworkId()) != null) {
                log.debug("AuthPCE.apply: Adding SDP " + sdp.getId());
                newTopo.addSdp(sdp);
            }
        }
        
        PCEData result = pceData;
        result.setTopology(newTopo);
        return result;
    }
}
