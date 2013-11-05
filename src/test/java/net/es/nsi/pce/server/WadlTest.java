/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.server;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.RestServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class WadlTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
      
        // Configure test instance of PCE server.
        try {
            ConfigurationManager.INSTANCE.initialize("src/test/resources/config/");
        } catch (Exception ex) {
            System.err.println("configure(): Could not initialize test environment." + ex.toString());
            fail("configure(): Could not initialize test environment.");
        }
        
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().getPackageName());
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }
    
    @Test
    public void testApplicationWadl() {
        WebTarget target = target().path("application.wadl");
        String serviceWadl = target.request(MediaType.APPLICATION_XML).get(String.class);
        assertTrue(serviceWadl.length() > 0);
    }
}
