package net.es.nsi.pce.test.config;


import net.es.nsi.pce.auth.AuthCredential;
import net.es.nsi.pce.config.JsonNsaConfigProvider;
import net.es.nsi.pce.config.NsaConfigAuthProvider;
import org.testng.annotations.Test;

import java.util.Map;

public class NsaConfigTest {
    @Test (groups = "config")
    public void testLoadConfig() throws Exception {
        System.out.println("testing NSA config");

        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();
        prov.loadConfig("src/test/resources/config/nsa.json");

        for (String nsaId: prov.getNsaIds()) {
            System.out.println("loaded config for nsa "+nsaId);
            System.out.println("network: "+prov.getConfig(nsaId).networkId);
            System.out.println("method: "+prov.getConfig(nsaId).auth.method);

        }
    }

    @Test (groups = "config")
    public void testAuthConfig() throws Exception {
        System.out.println("testing auth credential");
        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();
        prov.loadConfig("src/test/resources/config/nsa.json");
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
