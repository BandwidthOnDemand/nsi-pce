package net.es.nsi.pce.topology;

import java.util.List;
import net.es.nsi.pce.jersey.RestServer;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;


public class TopologyTest extends JerseyTest {
    final WebTarget root = target();
    final WebTarget topology = target().path("topology");
    
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
    public void testPing() {
        // Simple ping to determine if interface is available.
        Response response = topology.path("ping").request(MediaType.APPLICATION_JSON).get();
        
        System.out.println("Ping result " + response.getStatus());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testGetNsa() throws Exception {
        // Get a list of NSA.
        Response response = topology.path("nsas").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        // Run some model consistency checks.
        CollectionType collection = response.readEntity(CollectionType.class);
        
        List<NsaType> nsas = collection.getNsa();
        for (NsaType nsa : nsas) {
            // For each NSA retrieved we want to read the individual entry.
            response = topology.path("nsas/" + nsa.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            NsaType nsaGet = response.readEntity(NsaType.class);
            assertEquals(nsa.getId(), nsaGet.getId());
            
            List<ResourceRefType> networks = nsa.getNetwork();
            for (ResourceRefType network : networks) {
                response = root.path(network.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            }
        }
    }
    
    @Test
    public void testGetNetwork() throws Exception {
        // Get a list of networks.
        Response response = topology.path("networks").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        // We want to run some model consistency checks.  Test at most 50
        // entries otherwise this will take way too long.
        CollectionType collection = response.readEntity(CollectionType.class);
        List<NetworkType> networks = collection.getNetwork();
        int count = 0;
        for (NetworkType network : networks) {
            count++;
            if (count > 5) {
                break;
            }

            // For each Network retrieved we want to read the individual entry.
            response = topology.path("networks/" + network.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            NetworkType networkGet = response.readEntity(NetworkType.class);
            assertEquals(network.getId(), networkGet.getId());
            
            // Read the NSA entry.
            response = root.path(networkGet.getNsa().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            // Read the Services offered by this network.
            for (ResourceRefType service : networkGet.getService()) {
                response = root.path(service.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());                
            }
           
            // Read the STP exposed by this network.
            for (ResourceRefType stp : networkGet.getStp()) {
                response = root.path(stp.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            }
            
             // Read the ServiceDomain exposed by this network.
            for (ResourceRefType serviceDomian : networkGet.getServiceDomain()) {
                response = root.path(serviceDomian.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());                
            }

            // Read the Services exposed by this network.
            for (ResourceRefType service : networkGet.getService()) {
                response = root.path(service.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());                
            }
        }
    }
    
    @Test
    public void testGetServices() throws Exception {
        // Get a list of all STP.
        Response response = topology.path("services").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.
        CollectionType collection = response.readEntity(CollectionType.class);
        List<ServiceType> services = collection.getService();
        for (ServiceType service : services) {
            // For each Service retrieved we want to read the individual entry.
            response = root.path(service.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }   
    
    @Test
    public void testGetStp() throws Exception {
        // Get a list of all STP.
        Response response = topology.path("stps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.  Test at most 50
        // entries otherwise this will take way too long.
        CollectionType collection = response.readEntity(CollectionType.class);
        List<StpType> stps = collection.getStp();
        int count = 0;
        for (StpType stp : stps) {
            count++;
            if (count > 10) {
                break;
            }

            // For each STP retrieved we want to read the individual entry.
            response = topology.path("stps/" + stp.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            StpType stpGet = response.readEntity(StpType.class);
            assertEquals(stp.getId(), stpGet.getId());
            
            // Read the direct STP HREF.
            response = root.path(stpGet.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());            
            
            // Now verify the linked resources of this STP exist.
            response = topology.path("networks/" + stp.getNetworkId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            response = root.path(stp.getNetwork().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            response = root.path(stp.getServiceDomain().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            if (stp.getType() == StpDirectionalityType.BIDIRECTIONAL) {
                response = root.path(stp.getInboundStp().getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                
                response = root.path(stp.getOutboundStp().getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            }
        }
    }    
}
