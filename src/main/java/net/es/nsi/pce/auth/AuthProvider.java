package net.es.nsi.pce.auth;

import net.es.nsi.pce.api.AuthMethod;

import java.util.Map;


public interface AuthProvider {
    public AuthMethod getMethod(String nsaId);
    public Map<AuthCredential, String> getCredentials(String nsaId);
}
