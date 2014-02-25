package net.es.nsi.pce.config.nsa.auth;

import java.util.Map;

public interface AuthProvider {
    public AuthMethodType getMethod(String nsaId);
    public Map<AuthCredential, String> getCredentials(String nsaId);
}
