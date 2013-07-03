package net.es.nsi.pce.config.nsa;

import net.es.nsadb.nsa.beans.NsaRecord;
import net.es.nsadb.nsa.svc.api.NsaByFilterRequest;
import net.es.nsadb.nsa.svc.api.NsaListResponse;
import net.es.nsadb.nsa.svc.api.NsaProviderService;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbClientServiceInfoProvider implements ServiceInfoProvider {
    private String nsaDbUrl;


    private static DbClientServiceInfoProvider instance;
    public static DbClientServiceInfoProvider getInstance() {
        if (instance == null) {
            instance = new DbClientServiceInfoProvider();
        }
        return instance;
    }
    private DbClientServiceInfoProvider() {}


    public ServiceInfo byNsaId(String nsaId) {
        NsaProviderService nsaSvc = JAXRSClientFactory.create(nsaDbUrl, NsaProviderService.class);
        NsaByFilterRequest nsaReq = new NsaByFilterRequest();
        nsaReq.setNsaId(nsaId);
        NsaListResponse nsaResp = nsaSvc.byFilter(nsaReq);
        if (nsaResp.getNsaRecords().size() != 1) {
            System.err.println("could not find nsa service info for nsa id "+nsaId);
            return null;
        }
        NsaRecord rec = nsaResp.getNsaRecords().get(0);
        ServiceInfo result = new ServiceInfo();
        result.setNetworkId(rec.getNetworkId());
        result.setNsaId(rec.getNsaId());
        result.setProviderUrl(rec.getProviderUrl());
        return result;

    }
    public ServiceInfo byNetworkId(String networkId) {
        NsaProviderService nsaSvc = JAXRSClientFactory.create(nsaDbUrl, NsaProviderService.class);
        NsaByFilterRequest nsaReq = new NsaByFilterRequest();
        nsaReq.setNetworkId(networkId);
        NsaListResponse nsaResp = nsaSvc.byFilter(nsaReq);
        if (nsaResp.getNsaRecords().size() != 1) {
            System.err.println("could not find nsa service info for network id "+networkId);
            return null;
        }
        NsaRecord rec = nsaResp.getNsaRecords().get(0);
        ServiceInfo result = new ServiceInfo();
        result.setNetworkId(rec.getNetworkId());
        result.setNsaId(rec.getNsaId());
        result.setProviderUrl(rec.getProviderUrl());
        return result;
    }

    public Set<String> getNsaIds() {
        Set<String> res = new HashSet<>();
        NsaProviderService nsaSvc = JAXRSClientFactory.create(nsaDbUrl, NsaProviderService.class);
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
