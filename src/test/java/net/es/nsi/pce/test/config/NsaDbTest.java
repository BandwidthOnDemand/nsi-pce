package net.es.nsi.pce.test.config;


import net.es.nsadb.auth.beans.AuthRecord;
import net.es.nsadb.auth.beans.CredentialRecord;
import net.es.nsadb.auth.beans.CredentialType;
import net.es.nsadb.auth.svc.api.AuthByFilterRequest;
import net.es.nsadb.auth.svc.api.AuthListResponse;
import net.es.nsadb.auth.svc.api.AuthProviderService;
import net.es.nsadb.nsa.beans.NsaRecord;
import net.es.nsadb.nsa.svc.api.NsaByFilterRequest;
import net.es.nsadb.nsa.svc.api.NsaListResponse;
import net.es.nsadb.nsa.svc.api.NsaProviderService;
import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.NsaDbClientAuthProvider;
import net.es.nsi.pce.svc.api.AuthMethod;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NsaDbTest {
    private ApplicationContext context;

    @BeforeSuite (groups = {"nsadb", "spring"})
    public void loadSpring() {
        SpringContext sc = SpringContext.getInstance();
        context = sc.initContext("src/test/resources/config/beans.xml");
    }

    @Test (groups = "nsadb")
    public void testAuthConfig() throws Exception {
        System.out.println("**********\ntesting NSA db");

        NsaDbClientAuthProvider ap = (NsaDbClientAuthProvider) context.getBean("dbAuthProvider");

        String nsaId = "foo.net";
        Map<String, Long> ids;
        ids = this.insertRecords(ap, nsaId);

        AuthMethod am = ap.getMethod(nsaId);
        System.out.println(nsaId+ " method: "+am);


        Map<AuthCredential, String> creds = ap.getCredentials(nsaId);
        if (creds == null) {
            throw new Exception("null response");
        }
        for (AuthCredential ac : creds.keySet()) {
            if (creds.get(ac) != null) {
                System.out.println("   "+ac+ " ==> "+creds.get(ac));
            } else {
                System.out.println("   "+ac);
            }
        }
        /*
        ids = new HashMap<>();
        ids.put("auth", 5L);
        ids.put("nsa", 3L);
        */
        this.deleteRecords(ap, ids);

        System.out.println("testing NSA db done\n**********");

    }

    private Map<String, Long> insertRecords(NsaDbClientAuthProvider ap, String nsaId) throws Exception {

        String networkId = "fooNetworkId";

        String nsaDbUrl = ap.getNsaDbUrl();
        String authDbUrl = ap.getAuthDbUrl();
        System.out.println("nsaUrl: "+nsaDbUrl);
        System.out.println("authUrl: "+authDbUrl);
        if (nsaDbUrl == null) {
            throw new Exception("null nsa db url");
        }
        if (authDbUrl == null) {
            throw new Exception("null auth db url");
        }

        NsaProviderService nsaSvc = JAXRSClientFactory.create(nsaDbUrl, NsaProviderService.class);
        NsaRecord nsaRec = new NsaRecord();
        nsaRec.setNetworkId(networkId);
        nsaRec.setNsaId(nsaId);
        nsaRec.setProviderUrl("http://fooproviderurl.com/");
        nsaSvc.update(nsaRec);

        NsaByFilterRequest nsaReq = new NsaByFilterRequest();
        nsaReq.setNsaId(nsaId);
        NsaListResponse nsaResp = nsaSvc.byFilter(nsaReq);
        if (nsaResp.getNsaRecords().size() != 1) {
            throw new Exception("could not find network id for nsa id "+nsaId);
        }

        Long nsaRecordId = nsaResp.getNsaRecords().get(0).getId();
        System.out.println("nsa record id:"+nsaRecordId);


        AuthProviderService authSvc = JAXRSClientFactory.create(authDbUrl, AuthProviderService.class);
        AuthRecord arc = new AuthRecord();
        arc.setNetworkId(networkId);
        arc.setMethod(net.es.nsadb.auth.beans.AuthMethod.OAUTH2);
        CredentialRecord cr = new CredentialRecord();
        cr.setCredential("123131323");
        cr.setType(CredentialType.TOKEN);
        arc.getCredentialRecordSet().add(cr);
        authSvc.update(arc);

        AuthByFilterRequest authReq = new AuthByFilterRequest();
        authReq.setNetworkId(networkId);

        AuthListResponse authResp = authSvc.byFilter(authReq);
        List<AuthRecord> recs = authResp.getAuthRecords();
        if (recs.size() != 1) {
            throw new Exception("could not find auth record for nsa id "+nsaId);
        }
        Long authRecordId = recs.get(0).getId();
        System.out.println("auth record id:"+authRecordId);

        Map<String, Long> ids = new HashMap<>();
        ids.put("nsa", nsaRecordId);
        ids.put("auth", authRecordId);
        return ids;

    }

    private void deleteRecords(NsaDbClientAuthProvider ap, Map<String, Long> ids) {
        String nsaDbUrl = ap.getNsaDbUrl();
        String authDbUrl = ap.getAuthDbUrl();

        NsaProviderService nsaSvc = JAXRSClientFactory.create(nsaDbUrl, NsaProviderService.class);
        nsaSvc.delete(ids.get("nsa"));
        AuthProviderService authSvc = JAXRSClientFactory.create(authDbUrl, AuthProviderService.class);
        authSvc.delete(ids.get("auth"));

    }



}
