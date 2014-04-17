package net.es.nsi.pce.management;


import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.RestServer;
import net.es.nsi.pce.management.jaxb.LogListType;
import net.es.nsi.pce.management.jaxb.LogType;
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
public class LogsTest extends JerseyTest {
    private final static String CONFIG_DIR = "src/test/resources/config/";
    private static final String DEFAULT_TOPOLOGY_FILE = CONFIG_DIR + "topology-dds.xml";
    private static final String DEFAULT_DDS_FILE = CONFIG_DIR + "dds.xml";
    private static final String TOPOLOGY_CONFIG_FILE_ARGNAME = "topologyConfigFile";
    private static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";

    final WebTarget root = target();
    final WebTarget topology = target().path("management");
    
    @Override
    protected Application configure() {
        // Configure test instance of PCE server.
        System.setProperty(DDS_CONFIG_FILE_ARGNAME, DEFAULT_DDS_FILE);
        System.setProperty(TOPOLOGY_CONFIG_FILE_ARGNAME, DEFAULT_TOPOLOGY_FILE);
        try {
            ConfigurationManager.INSTANCE.initialize("src/test/resources/config/");
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
    public void getAllLogs() {
        Response response = topology.path("logs").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        final ChunkedInput<LogListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogListType>>() {});
        LogListType chunk;
        LogListType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            finalTopology = chunk;
        }
        
        assertNotNull(finalTopology);
        
        int count = 0;
        for (LogType log : finalTopology.getLog()) {
            response = topology.path("logs/" + log.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            LogType readLog = response.readEntity(LogType.class);
            System.out.println("Read log: " + readLog.getId());
            
            // Limit the number we retrieve otherwise build will take forever.
            count++;
            if (count > 20) {
                break;
            }
        }
    }
    
    @Test
    public void getTypeFilteredLogs() {
        Response response = topology.path("logs").queryParam("type", "Log").queryParam("code", "1001").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        final ChunkedInput<LogListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogListType>>() {});
        LogListType chunk;
        LogListType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            finalTopology = chunk;
        }
        
        assertNotNull(finalTopology);
    }
    
    @Test
    public void getLabelFilteredLogs() {
        Response response = topology.path("logs").queryParam("label", "AUDIT_SUCCESSFUL").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        LogListType logs = response.readEntity(LogListType.class);

        for (LogType log : logs.getLog()) {
            response = topology.path("logs").queryParam("audit", log.getAudit().toXMLFormat()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            final ChunkedInput<LogListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogListType>>() {});
            LogListType chunk;
            LogListType finalTopology = null;
            while ((chunk = chunkedInput.read()) != null) {
                System.out.println("Chunk received...");
                finalTopology = chunk;
            }

            assertNotNull(finalTopology);
        }
    }

    @Test
    public void badFilter() {
        Response response = topology.path("logs").queryParam("code", "1001").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        response = topology.path("logs").queryParam("type", "POOP").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        response = topology.path("logs").path("666").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
