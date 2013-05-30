package net.es.nsi.pce.config.nsa.auth;

import net.es.nsi.pce.svc.api.AuthMethod;

import java.util.Map;


public interface AuthProvider {
    public AuthMethod getMethod(String nsaId);
    public Map<AuthCredential, String> getCredentials(String nsaId);
}
