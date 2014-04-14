package net.es.nsi.pce.pf;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.config.nsa.auth.AuthMethodType;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.springframework.context.ApplicationContext;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


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
        return pceData;
    }
    
    /*
    public PCEData apply(PCEData pceData) throws Exception {
        log.debug("AuthPCE.apply: Starting ...");
        
        // Get the Spring context and the required providers.
        SpringContext sc  = SpringContext.getInstance();
        ApplicationContext context = sc.getContext();
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");

        // Get the current topology which we will filter into our new topology.
        NsiTopology topology = pceData.getTopology();
        NsiTopology newTopology = new NsiTopology();
        
        // Check to see if we have credentials for communicating directly with
        // the target network.
        
        // TODO: When we support control plane peering topology this pruning
        // will not be required.
        for (String nsaId : topology.getNsaIds()) {
            ServiceInfo si = sip.byNsaId(nsaId);
            if (si == null) {
                log.error("AuthPCE.apply: Could not find service info for NSA Id " + nsaId + ", not including it in topology.");
            } else {
                AuthMethodType method = ap.getMethod(nsaId);
                Map<AuthCredential, String> credentials = ap.getCredentials(nsaId);

                // Discard the network if we do not have a credentials entry.
                if (method == null) {
                    log.error("AuthPCE.apply: No auth method known for NSA Id " + nsaId + ", not including it in topology.");
                }
                else if (method.equals(AuthMethodType.NONE)) {
                    log.debug("AuthPCE.apply: Adding nsaId = " + nsaId);
                    newTopology.addNsa(topology.getNsa(nsaId));
                }
                else if (credentials == null || credentials.isEmpty()) {
                    log.error("AuthPCE.apply: No credentials for NSA Id " + nsaId + ", not including it in topology");
                }
                else {
                    log.debug("AuthPCE.apply: Adding nsaId = " + nsaId);
                    newTopology.addNsa(topology.getNsa(nsaId));
                }             
            }           
        }
        
        // Now we need to copy the associated networks to the new topology.
        for (NsaType nsa : newTopology.getNsas()) {
            for (ResourceRefType networkRef : nsa.getNetwork()) {
                NetworkType network = topology.getNetworkById(networkRef.getId());
                if (network != null) {
                    //log.debug("AuthPCE.apply: Adding Network " + network.getId());
                    newTopology.addNetwork(network);
                    
                    // Now the STPs associated with this network.
                    for (ResourceRefType stpRef : network.getStp()) {
                        StpType stp = topology.getStp(stpRef.getId());
                        if (stp != null) {
                            //log.debug("AuthPCE.apply: Adding STP " + stp.getId());
                            newTopology.addStp(stp);
                        }
                    }
                    
                    // The Service Domains.
                    for (ResourceRefType tsRef : network.getServiceDomain()) {
                        ServiceDomainType ts = topology.getServiceDomain(tsRef.getId());
                        if (ts != null) {
                            //log.debug("AuthPCE.apply: Adding TransferFunction " + ts.getId());
                            newTopology.addServiceDomain(ts);
                        }
                    }
                    
                    // The services.
                    for (ResourceRefType serviceRef : network.getService()) {
                        ServiceType service = topology.getService(serviceRef.getId());
                        if (service != null) {
                            //log.debug("AuthPCE.apply: Adding Service " + service.getId());
                            newTopology.addService(service);
                        }
                    }                    
                }
            }            
        }
        
        // Now we prune bidirectional SDP from the list that have had their
        // networks removed.
        for (SdpType sdp : topology.getSdps()) {
            if (newTopology.getNetworkById(sdp.getDemarcationA().getNetwork().getId()) != null &&
                    newTopology.getNetworkById(sdp.getDemarcationZ().getNetwork().getId()) != null) {
                newTopology.addSdp(sdp);
            }
        }
        
        PCEData result = pceData;
        result.setTopology(newTopology);
        
        log.debug("AuthPCE.apply: ... Finished.");
        
        return result;
    }*/
}
