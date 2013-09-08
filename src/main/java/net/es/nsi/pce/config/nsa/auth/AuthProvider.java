package net.es.nsi.pce.config.nsa.auth;


import java.util.Map;
import net.es.nsi.pce.api.jaxb.AuthMethodType;


public interface AuthProvider {
    public AuthMethodType getMethod(String nsaId);
    public Map<AuthCredential, String> getCredentials(String nsaId);
}
