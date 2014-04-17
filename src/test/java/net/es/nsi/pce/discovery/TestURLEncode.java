/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.discovery.actors.DdsActorSystem;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author hacksaw
 */
public class TestURLEncode {
    private static final String configDir = "src/test/resources/config/";
    private static final String beanConfig = new StringBuilder(configDir).append("beans.xml").toString().replace("/", File.separator);
        private static final String log4jConfig = new StringBuilder(configDir).append("log4j.xml").toString().replace("/", File.separator);
        
    public static void main(String[] args) throws Exception {
        DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);
        
        Logger log = LoggerFactory.getLogger(TestURLEncode.class);
        
        String url = "application/vnd.ogf.nsi.topology.v2+xml";
        System.out.println(url);
        url = URLEncoder.encode(url, "UTF-8");
        System.out.println(url);
        url = URLDecoder.decode(url, "UTF-8");
        System.out.println(url);
        
        // Get a reference to the topology provider through spring.
        SpringContext sc = SpringContext.getInstance();
        log.debug("TestConfig: loading beanConfig=" + beanConfig);
        ApplicationContext context;
        try {
            context = sc.initContext(beanConfig);
        }
        catch (Exception ex) {
            System.err.println("TestConfig: initContext failed");
            ex.printStackTrace();
            return;
        }
        System.out.println("TestConfig: remoteSubscriptionCache");
        context.getBean("remoteSubscriptionCache");
        System.out.println("TestConfig: httpConfigProvider");
        context.getBean("httpConfigProvider");
        System.out.println("TestConfig: serviceInfoProvider");
        context.getBean("serviceInfoProvider");
        System.out.println("TestConfig: discoveryProvider");
        context.getBean("discoveryProvider");
        System.out.println("TestConfig: topologyProvider");
        context.getBean("topologyProvider");
        Thread.sleep(60*1000);
        System.out.println("TestConfig: ddsActorSystem");
        DdsActorSystem actorSystem = (DdsActorSystem) context.getBean("ddsActorSystem");
        actorSystem.shutdown();
    }
}
