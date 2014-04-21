package net.es.nsi.pce.config.nsa.auth;

import net.es.nsi.pce.spring.SpringContext;
import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
import net.es.nsi.pce.config.nsa.NsaConfig;

import java.util.HashMap;
import java.util.Map;

public class NsaConfigAuthProvider implements AuthProvider {


    @Override
    public AuthMethodType getMethod(String nsaId) {
        SpringContext sc = SpringContext.getInstance();
        JsonNsaConfigProvider ncp = (JsonNsaConfigProvider) sc.getContext().getBean("serviceInfoProvider");
        NsaConfig nc = ncp.getConfig(nsaId);
        if (nc == null) {
            return null;
        }
        if (nc.auth == null) {
            return null;
        }

        return nc.auth.method;
    }

    @Override
    public Map<AuthCredential, String> getCredentials(String nsaId) {
        SpringContext sc = SpringContext.getInstance();
        JsonNsaConfigProvider ncp = (JsonNsaConfigProvider) sc.getContext().getBean("serviceInfoProvider");
        NsaConfig nc = ncp.getConfig(nsaId);
        if (nc == null) {
            System.err.println("no config for nsa id "+nsaId);
            return null;
        }

        AuthConfig ac = nc.auth;
        HashMap<AuthCredential, String> res = new HashMap<>();
        if (ac.username != null) {
            res.put(AuthCredential.USERNAME, ac.username);
        }
        if (ac.password != null) {
            res.put(AuthCredential.PASSWORD, ac.password);
        }
        if (ac.token != null) {
            res.put(AuthCredential.TOKEN, ac.token);
        }
        return res;
    }
}
