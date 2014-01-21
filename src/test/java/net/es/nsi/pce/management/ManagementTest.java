package net.es.nsi.pce.management;


import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.RestServer;
import net.es.nsi.pce.managemenet.jaxb.LogType;
import net.es.nsi.pce.managemenet.jaxb.LogsType;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
public class ManagementTest extends JerseyTest {
    final WebTarget root = target();
    final WebTarget topology = target().path("management");
    
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
        Response response = topology.path("status").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testlogs() {
        Response response = topology.path("logs").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        final ChunkedInput<LogsType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogsType>>() {});
        LogsType chunk;
        LogsType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            finalTopology = chunk;
        }
        
        assertNotNull(finalTopology);
        
        int count = 0;
        for (LogType log : finalTopology.getLog()) {
            response = topology.path("logs/" + log.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            // Limit the number we retrieve otherwise build will take forever.
            count++;
            if (count > 20) {
                break;
            }
        }
    }
}
