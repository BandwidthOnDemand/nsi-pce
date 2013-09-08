package net.es.nsi.pce.config.nsa.auth;

import net.es.nsadb.auth.beans.AuthRecord;
import net.es.nsadb.auth.beans.CredentialRecord;
import net.es.nsadb.auth.beans.CredentialType;
import net.es.nsadb.auth.svc.api.AuthByFilterRequest;
import net.es.nsadb.auth.svc.api.AuthListResponse;
import net.es.nsadb.auth.svc.api.AuthProviderService;
import net.es.nsadb.nsa.svc.api.NsaByFilterRequest;
import net.es.nsadb.nsa.svc.api.NsaListResponse;
import net.es.nsadb.nsa.svc.api.NsaProviderService;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.es.nsi.pce.api.jaxb.AuthMethodType;

public class DbClientAuthProvider implements AuthProvider {
    private String nsaDbUrl;
    private String authDbUrl;

    @Override
    public AuthMethodType getMethod(String nsaId) {

        String networkId = this.getNetworkId(nsaId);
        if (networkId == null) {
            return null;
        }

        AuthProviderService authSvc = JAXRSClientFactory.create(authDbUrl, AuthProviderService.class);
        AuthByFilterRequest authReq = new AuthByFilterRequest();
        authReq.setNetworkId(networkId);
        AuthListResponse authResp = authSvc.byFilter(authReq);
        List<AuthRecord> recs = authResp.getAuthRecords();
        if (recs.size() != 1) {
            return null;
        }
        net.es.nsadb.auth.beans.AuthMethod am = recs.get(0).getMethod();
        if (am.equals(net.es.nsadb.auth.beans.AuthMethod.BASIC)) {
            return AuthMethodType.BASIC;
        } else if (am.equals(net.es.nsadb.auth.beans.AuthMethod.NONE)) {
            return AuthMethodType.NONE;

        } else if (am.equals(net.es.nsadb.auth.beans.AuthMethod.OAUTH2)) {
            return AuthMethodType.OAUTH_2;
        }

        return null;


    }

    @Override
    public Map<AuthCredential, String> getCredentials(String nsaId) {
        String networkId = this.getNetworkId(nsaId);

        if (networkId == null) {
            return null;
        }
        AuthProviderService authSvc = JAXRSClientFactory.create(authDbUrl, AuthProviderService.class);
        AuthByFilterRequest authReq = new AuthByFilterRequest();
        authReq.setNetworkId(networkId);

        AuthListResponse authResp = authSvc.byFilter(authReq);
        List<AuthRecord> recs = authResp.getAuthRecords();
        if (recs.size() != 1) {
            return null;
        }
        Set<CredentialRecord> creds = recs.get(0).getCredentialRecordSet();
        Map<AuthCredential, String> result = new HashMap<>();
        for (CredentialRecord cred : creds) {
            CredentialType t = cred.getType();
            String c = cred.getCredential();
            AuthCredential ac;
            if (t.equals(CredentialType.PASSWORD)) {
                ac = AuthCredential.PASSWORD;
            } else if (t.equals(CredentialType.TOKEN)) {

                ac = AuthCredential.TOKEN;
            } else if (t.equals(CredentialType.USERNAME)) {
                ac = AuthCredential.USERNAME;
            } else {
                return  null;
            }
            result.put(ac, c);
        }
        return  result;

    }

    private String getNetworkId(String nsaId) {
        NsaProviderService nsaSvc = JAXRSClientFactory.create(nsaDbUrl, NsaProviderService.class);
        NsaByFilterRequest nsaReq = new NsaByFilterRequest();
        nsaReq.setNsaId(nsaId);
        NsaListResponse nsaResp = nsaSvc.byFilter(nsaReq);
        if (nsaResp.getNsaRecords().size() != 1) {
            System.err.println("could not find network id for nsa id "+nsaId);
            return null;
        }
        String networkId = nsaResp.getNsaRecords().get(0).getNetworkId();
        return networkId;
    }


    public String getNsaDbUrl() {
        return nsaDbUrl;
    }

    public void setNsaDbUrl(String nsaDbUrl) {
        System.out.println("setNsaDbUrl: " + nsaDbUrl);
        this.nsaDbUrl = nsaDbUrl;
    }

    public String getAuthDbUrl() {
        return authDbUrl;
    }

    public void setAuthDbUrl(String authDbUrl) {
        System.out.println("setAuthDbUrl: "+ authDbUrl);
        this.authDbUrl = authDbUrl;
    }
}
