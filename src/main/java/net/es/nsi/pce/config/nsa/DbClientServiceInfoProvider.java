package net.es.nsi.pce.config.nsa;

import net.es.nsadb.nsa.beans.NsaRecord;
import net.es.nsadb.nsa.svc.api.NsaByFilterRequest;
import net.es.nsadb.nsa.svc.api.NsaListResponse;
import net.es.nsadb.nsa.svc.api.NsaProviderService;
import org.glassfish.jersey.client.proxy.WebResourceFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbClientServiceInfoProvider implements ServiceInfoProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
        
    private String nsaDbUrl;

    private DbClientServiceInfoProvider() {}
    
    private static class SingletonHolder { 
        public static final DbClientServiceInfoProvider INSTANCE = new DbClientServiceInfoProvider();
    }
            
    public static DbClientServiceInfoProvider getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Retrieve ServiceInfo from the external database based on the provided
     * NSA identifier.
     * 
     * @param nsaId The NSA identifier for the desired ServiceInfo.
     * @return ServiceInfo matching the requested NSA.
     */
    @Override
    public ServiceInfo byNsaId(String nsaId) {
       
        // Create our proxy client.
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(nsaDbUrl);
        NsaProviderService nsaSvc = WebResourceFactory.newResource(NsaProviderService.class, webTarget);

        // Build the request filter.
        NsaByFilterRequest nsaReq = new NsaByFilterRequest();
        nsaReq.setNsaId(nsaId);
        
        // Invoke the remote API call.
        NsaListResponse nsaResp = nsaSvc.byFilter(nsaReq);
        if (nsaResp.getNsaRecords().size() != 1) {
            log.error("byNsaId: Could not find nsa service info for nsa id " + nsaId);
            return null;
        }
        
        NsaRecord rec = nsaResp.getNsaRecords().get(0);
        ServiceInfo result = new ServiceInfo();
        result.setNetworkId(rec.getNetworkId());
        result.setNsaId(rec.getNsaId());
        result.setProviderUrl(rec.getProviderUrl());
        return result;

    }
    
    @Override
    public ServiceInfo byNetworkId(String networkId) {
        
        // Create our proxy client.
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(nsaDbUrl);
        NsaProviderService nsaSvc = WebResourceFactory.newResource(NsaProviderService.class, webTarget);
        
        NsaByFilterRequest nsaReq = new NsaByFilterRequest();
        nsaReq.setNetworkId(networkId);
        NsaListResponse nsaResp = nsaSvc.byFilter(nsaReq);
        if (nsaResp.getNsaRecords().size() != 1) {
            log.error("byNetworkId: Could not find nsa service info for network id " + networkId);
            return null;
        }
        NsaRecord rec = nsaResp.getNsaRecords().get(0);
        ServiceInfo result = new ServiceInfo();
        result.setNetworkId(rec.getNetworkId());
        result.setNsaId(rec.getNsaId());
        result.setProviderUrl(rec.getProviderUrl());
        return result;
    }

    @Override
    public Set<String> getNsaIds() {

        // Create our proxy client.
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(nsaDbUrl);
        NsaProviderService nsaSvc = WebResourceFactory.newResource(NsaProviderService.class, webTarget);

        Set<String> res = new HashSet<>();
        NsaListResponse resp = nsaSvc.list();
        List<NsaRecord> recs = resp.getNsaRecords();
        for (NsaRecord rec : recs) {
            res.add(rec.getNsaId());
        }
        return res;
    }

    public String getNsaDbUrl() {
        return nsaDbUrl;
    }

    public void setNsaDbUrl(String nsaDbUrl) {
        this.nsaDbUrl = nsaDbUrl;
    }

}
