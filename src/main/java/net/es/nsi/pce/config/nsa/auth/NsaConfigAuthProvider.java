package net.es.nsi.pce.config.nsa.auth;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.NsaConfig;
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
    }

    public AuthMethod getMethod(String nsaId) {
        SpringContext sc = SpringContext.getInstance();
        NsaConfigProvider ncp = (NsaConfigProvider) sc.getContext().getBean("nsaConfigProvider");
        NsaConfig nc = ncp.getConfig(nsaId);
        if (nc == null) {
            return null;
        }
        if (nc.auth == null) {
            return null;
        }

        return nc.auth.method;
    }

    public Map<AuthCredential, String> getCredentials(String nsaId) {
        SpringContext sc = SpringContext.getInstance();
        NsaConfigProvider ncp = (NsaConfigProvider) sc.getContext().getBean("nsaConfigProvider");
        NsaConfig nc = ncp.getConfig(nsaId);
        if (nc == null) {
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
