package net.es.nsi.pce.test.config;


import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
import net.es.nsi.pce.config.nsa.auth.NsaConfigAuthProvider;
import org.testng.annotations.Test;

import java.util.Map;

public class NsaConfigTest {
    @Test (groups = "config")
    public void testLoadConfig() throws Exception {
        System.out.println("testing NSA config");

        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();
        prov.setFilename("src/test/resources/config/nsa.json");
        prov.loadConfig();

        for (String nsaId: prov.getNsaIds()) {
            System.out.println("loaded config for nsa "+nsaId);
            System.out.println("Network: "+prov.getConfig(nsaId).networkId);
            System.out.println("method: "+prov.getConfig(nsaId).auth.method);

        }
    }

    @Test (groups = "config")
    public void testAuthConfig() throws Exception {
        System.out.println("testing auth credential");
        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();
        prov.setFilename("src/test/resources/config/nsa.json");
        prov.loadConfig();
        NsaConfigAuthProvider ap = NsaConfigAuthProvider.getInstance();

        for (String nsaId: prov.getNsaIds()) {
            System.out.println("credentials for nsa "+nsaId);
            Map<AuthCredential, String> creds = ap.getCredentials(nsaId);
            for (AuthCredential cred : creds.keySet()) {
                System.out.println("---"+cred + " -> "+creds.get(cred));
            }


        }


    }
}
