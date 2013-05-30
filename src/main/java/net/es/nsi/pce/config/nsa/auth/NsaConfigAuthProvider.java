package net.es.nsi.pce.config.nsa.auth;

import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
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


    public AuthMethod getMethod(String nsaId) {
        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();
        return prov.getConfig(nsaId).auth.method;
    }

    public Map<AuthCredential, String> getCredentials(String nsaId) {
        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();

        AuthConfig ac = prov.getConfig(nsaId).auth;
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
