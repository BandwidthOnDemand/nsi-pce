package net.es.nsi.pce.topology;

import java.util.Date;
import java.util.List;
import net.es.nsi.pce.jersey.RestServer;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ChunkedInput;


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
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testAllTopology() throws Exception {
        // Get a list of all topology resources.
        Response response = topology.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        // The test topology is too big for a single blocking read so we need
        // to use the jersey client chunked read.  It would appear that a single
        // read is all that is required for this JSON based message.
        final ChunkedInput<CollectionType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<CollectionType>>() {});
        CollectionType chunk;
        CollectionType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            finalTopology = chunk;
        }
        
        assertNotNull(finalTopology);
        
        System.out.println("Nsa: " + finalTopology.getNsa().size());
        System.out.println("Networks: " + finalTopology.getNetwork().size());
        System.out.println("Stps: " + finalTopology.getStp().size());
        System.out.println("Services: " + finalTopology.getService().size());
        System.out.println("ServiceDomains: " + finalTopology.getServiceDomain().size());
        System.out.println("ServiceAdaptations: " + finalTopology.getServiceAdaptation().size());
        System.out.println("Sdps: " + finalTopology.getSdp().size());
        
        // Verify the complete topology retrieval matches the individual ones.

        // Get a list of NSA.
        response = topology.path("nsas").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        CollectionType collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getNsa().size(), finalTopology.getNsa().size());
        
        // Get a list of networks.
        response = topology.path("networks").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getNetwork().size(), finalTopology.getNetwork().size());

        // Get a list of all Services.
        response = topology.path("services").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getService().size(), finalTopology.getService().size());
        
        // Get a list of all ServicesDomains.
        response = topology.path("serviceDomains").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getServiceDomain().size(), finalTopology.getServiceDomain().size());
        
        // Get a list of all ServiceAdaptations.
        response = topology.path("serviceAdaptations").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getServiceAdaptation().size(), finalTopology.getServiceAdaptation().size());
        
        // Get a list of all STP.
        response = topology.path("stps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getStp().size(), finalTopology.getStp().size());
        
        // Get a list of all SDP.
        response = topology.path("sdps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        collection = response.readEntity(CollectionType.class);
        assertEquals(collection.getSdp().size(), finalTopology.getSdp().size());
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
    public void testGetServiceDomains() throws Exception {
        // Get a list of all ServiceDomains.
        Response response = topology.path("serviceDomains").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.
        CollectionType collection = response.readEntity(CollectionType.class);
        List<ServiceDomainType> serviceDomains = collection.getServiceDomain();
        
        int count = 0;
        for (ServiceDomainType serviceDomain : serviceDomains) {
            // Limit iterations to avoind long builds.
            count++;
            if (count > 10) {
                break;
            }
            
            // For each ServiceDomain retrieved we want to read the individual entry.
            response = root.path(serviceDomain.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            // Now the linked resources.
            ServiceDomainType serviceDomainGet = response.readEntity(ServiceDomainType.class);
            assertEquals(serviceDomain.getId(), serviceDomainGet.getId());
            
            response = root.path(serviceDomainGet.getService().getHref()).request(MediaType.APPLICATION_JSON).get();
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
    
    @Test
    public void testGetStpByNetworkId() throws Exception {
        // Get a list of all STP filtered by Network Id.
        Response response = topology.path("stps").queryParam("networkId", "urn:ogf:network:uvalight.net:2013:topology").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get the returned list so we can count the number of entries.
        CollectionType collection = response.readEntity(CollectionType.class);
        int filteredStpSize = collection.getStp().size();
        
        // Get all the STP under the same network using a network rooted query.
        response = topology.path("networks/urn:ogf:network:uvalight.net:2013:topology/stps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        collection = response.readEntity(CollectionType.class);
        assertEquals(filteredStpSize, collection.getStp().size());
    }
    
    @Test
    public void testGetSdp() throws Exception {
        // Get a list of all STP.
        Response response = topology.path("sdps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.  Test at most 10
        // entries otherwise this will take way too long.
        CollectionType collection = response.readEntity(CollectionType.class);
        List<SdpType> sdps = collection.getSdp();
        int count = 0;
        for (SdpType sdp : sdps) {
            count++;
            if (count > 10) {
                break;
            }

            // For each SDP retrieved we want to read the individual entry.
            response = topology.path("sdps/" + sdp.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            StpType sdpGet = response.readEntity(StpType.class);
            assertEquals(sdp.getId(), sdpGet.getId());
            
            // Read the direct SDP HREF.
            response = root.path(sdpGet.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());            
            
            // Now verify the linked resources of this SDP exist.
            response = root.path(sdp.getDemarcationA().getStp().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            response = root.path(sdp.getDemarcationA().getServiceDomain().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = root.path(sdp.getDemarcationA().getNetwork().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            response = root.path(sdp.getDemarcationZ().getStp().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            response = root.path(sdp.getDemarcationZ().getServiceDomain().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = root.path(sdp.getDemarcationZ().getNetwork().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testLastModified() throws Exception {
        // Get a specific STP.
        Response response = topology.path("stps/urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:526?vlan=1784").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        Date lastMod = response.getLastModified();
        
        response = topology.path("stps/urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:526?vlan=1784").request(MediaType.APPLICATION_XML).header("If-Modified-Since", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123)).get();
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
        
        // Get a list of all topology resources.
        response = topology.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        // The test topology is too big for a single blocking read so we need
        // to use the jersey client chunked read.  It would appear that a single
        // read is all that is required for this JSON based message.
        final ChunkedInput<CollectionType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<CollectionType>>() {});
        CollectionType chunk;
        CollectionType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            finalTopology = chunk;
        }
        
        assertNotNull(finalTopology);
        
        lastMod = response.getLastModified();
        
        response = topology.request(MediaType.APPLICATION_JSON).header("If-Modified-Since", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123)).get();
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());        
    }
}
