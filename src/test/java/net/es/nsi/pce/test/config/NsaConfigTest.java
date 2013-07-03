package net.es.nsi.pce.test.config;


import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import org.springframework.context.ApplicationContext;
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
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
        for (String nsaId: sip.getNsaIds()) {
            ServiceInfo si = sip.byNsaId(nsaId);
            System.out.println("NSA config:\n   nsa id:"+nsaId);
            System.out.println("   network: "+si.getNetworkId());

        }
    }

    @Test (groups = "config")
    public void testAuthConfig() throws Exception {

        System.out.println("testing auth credential");
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
        AuthProvider ap = (AuthProvider) context.getBean("authProvider");

        for (String nsaId: sip.getNsaIds()) {
            System.out.println("credentials for nsa "+nsaId);
            Map<AuthCredential, String> creds = ap.getCredentials(nsaId);
            for (AuthCredential cred : creds.keySet()) {
                System.out.println("---"+cred + " -> "+creds.get(cred));
            }


        }
    }


}
