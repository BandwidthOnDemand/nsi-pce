package net.es.nsi.pce.management;


import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.RestServer;
import net.es.nsi.pce.managemenet.jaxb.StatusType;
import net.es.nsi.pce.managemenet.jaxb.TopologyStatusType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hacksaw
 */
public class StatusTest extends JerseyTest {
    final WebTarget root = target();
    final WebTarget topology = target().path("management/status/");
    
    @Override
    protected Application configure() {
        // Configure test instance of PCE server.
        try {
            ConfigurationManager.INSTANCE.initialize("src/test/resources/config-SwitchingService/");
        } catch (Exception ex) {
            System.err.println("configure(): Could not initialize test environment." + ex.toString());
            fail("configure(): Could not initialize test environment.");
        }
        Application app = new Application();
        app.getProperties();
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().getPackageName());
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }
    
    @Test
    public void testStatus() {
        // Simple status to determine current state of topology discovery.
        Response response = topology.path("topology").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        StatusType status = response.readEntity(StatusType.class);
        System.out.println("Status code = " + status.getStatus());
        assertEquals(TopologyStatusType.COMPLETED, status.getStatus());
        System.out.println("Topology status code = " + status.getStatus().value());
    }
}
