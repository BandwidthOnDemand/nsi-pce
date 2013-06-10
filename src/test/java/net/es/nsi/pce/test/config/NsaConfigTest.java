package net.es.nsi.pce.test.config;


import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.NsaConfigProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.config.nsa.auth.NsaConfigAuthProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Map;

public class NsaConfigTest {
    private ApplicationContext context;

    @BeforeSuite (groups = {"config", "spring"})
    public void loadSpring() {
        SpringContext sc = SpringContext.getInstance();
        context = sc.initContext("src/test/resources/config/beans.xml");
    }

    @Test (groups = "config")
    public void testLoadConfig() throws Exception {
        System.out.println("testing NSA config");
        NsaConfigProvider ncp = (NsaConfigProvider) context.getBean("nsaConfigProvider");


        for (String nsaId: ncp.getNsaIds()) {
            System.out.println("loaded config for nsa "+nsaId);
            System.out.println("Network: "+ncp.getConfig(nsaId).networkId);
            System.out.println("method: "+ncp.getConfig(nsaId).auth.method);

        }
    }

    @Test (groups = "config")
    public void testAuthConfig() throws Exception {
        System.out.println("testing auth credential");

        NsaConfigProvider ncp = (NsaConfigProvider) context.getBean("nsaConfigProvider");
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");

        for (String nsaId: ncp.getNsaIds()) {
            System.out.println("credentials for nsa "+nsaId);
            Map<AuthCredential, String> creds = ap.getCredentials(nsaId);
            for (AuthCredential cred : creds.keySet()) {
                System.out.println("---"+cred + " -> "+creds.get(cred));
            }


        }
    }


}
