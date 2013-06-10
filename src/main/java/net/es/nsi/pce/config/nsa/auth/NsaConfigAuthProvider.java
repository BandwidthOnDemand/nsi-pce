package net.es.nsi.pce.config.nsa.auth;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
import net.es.nsi.pce.config.nsa.NsaConfigProvider;
import net.es.nsi.pce.svc.api.AuthMethod;

import java.util.HashMap;
import java.util.Map;

public class NsaConfigAuthProvider implements AuthProvider {
    private static NsaConfigAuthProvider instance;
    private NsaConfigAuthProvider() {}
    public static NsaConfigAuthProvider getInstance() {
        if (instance == null) {
            instance = new NsaConfigAuthProvider();
        }
        return instance;
    }


    public void afterPropertiesSet() throws Exception {
        return;
    }

    public AuthMethod getMethod(String nsaId) {
        SpringContext sc = SpringContext.getInstance();
        NsaConfigProvider ncp = (NsaConfigProvider) sc.getContext().getBean("nsaConfigProvider");

        return ncp.getConfig(nsaId).auth.method;
    }

    public Map<AuthCredential, String> getCredentials(String nsaId) {
        SpringContext sc = SpringContext.getInstance();
        NsaConfigProvider ncp = (NsaConfigProvider) sc.getContext().getBean("nsaConfigProvider");

        AuthConfig ac = ncp.getConfig(nsaId).auth;
        HashMap<AuthCredential, String> res = new HashMap<AuthCredential, String>();
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
