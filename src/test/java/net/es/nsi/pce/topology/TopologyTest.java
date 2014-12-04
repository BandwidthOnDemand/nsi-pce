package net.es.nsi.pce.topology;

import java.util.Date;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.gson.JsonProxy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import net.es.nsi.pce.test.TestConfig;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.ServiceAdaptationType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import org.apache.http.client.utils.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;


public class TopologyTest {
    private static TestConfig testConfig;
    private static Client client;
    private static WebTarget topology;
    private static JsonProxy proxy = new JsonProxy();

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** TopologyTest oneTimeSetUp ***********************************");
        // Configure the local test client callback server.
        testConfig = new TestConfig();
        topology = testConfig.getTarget().path("topology");
        client = testConfig.getClient();
        System.out.println("*************************************** TopologyTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** TopologyTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        System.out.println("*************************************** TopologyTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testPing() throws Exception {
        // Simple ping to determine if interface is available.
        Response response = topology.path("ping").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testAllTopology() throws Exception {
        // Get a list of all topology resources.
        Response response = topology.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        CollectionType finalTopology = proxy.deserialize(response, CollectionType.class);
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
        System.out.println("\n\n\n\n\n\n\n\n\n****************************************************************");
        response = topology.path("nsas").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<NsaType> nsas = proxy.deserializeList(response, NsaType.class);
        assertNotNull(nsas);
        assertEquals(nsas.size(), finalTopology.getNsa().size());

        // Get a list of networks.
        response = topology.path("networks").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<NetworkType> networks = proxy.deserializeList(response, NetworkType.class);
        assertNotNull(networks);
        assertEquals(networks.size(), finalTopology.getNetwork().size());

        // Get a list of all Services.
        response = topology.path("services").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ServiceType> services = proxy.deserializeList(response, ServiceType.class);
        assertEquals(services.size(), finalTopology.getService().size());

        // Get a list of all ServicesDomains.
        response = topology.path("serviceDomains").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ServiceDomainType> serviceDomains = proxy.deserializeList(response, ServiceDomainType.class);
        assertNotNull(serviceDomains);
        assertEquals(serviceDomains.size(), finalTopology.getServiceDomain().size());

        // Get a list of all ServiceAdaptations.
        response = topology.path("serviceAdaptations").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ServiceAdaptationType> serviceAdaptations = proxy.deserializeList(response, ServiceAdaptationType.class);
        assertNotNull(serviceAdaptations);
        assertEquals(serviceAdaptations.size(), finalTopology.getServiceAdaptation().size());

        // Get a list of all STP.
        response = topology.path("stps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<StpType> stps = proxy.deserializeList(response, StpType.class);
        assertNotNull(stps);
        assertEquals(stps.size(), finalTopology.getStp().size());

        // Get a list of all SDP.
        response = topology.path("sdps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<SdpType> sdps = proxy.deserializeList(response, SdpType.class);
        assertNotNull(sdps);
        assertEquals(sdps.size(), finalTopology.getSdp().size());
    }

    @Test
    public void testGetNsa() throws Exception {
        // Get a list of NSA.
        Response response = topology.path("nsas").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Run some model consistency checks.
        List<NsaType> nsaList = proxy.deserializeList(response, NsaType.class);

        for (NsaType nsa : nsaList) {
            // For each NSA retrieved we want to read the individual entry.
            response = topology.path("nsas/" + nsa.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            NsaType nsaGet = proxy.deserialize(response, NsaType.class);
            assertEquals(nsa.getId(), nsaGet.getId());

            List<ResourceRefType> networks = nsa.getNetwork();
            for (ResourceRefType network : networks) {
                response = topology.path(network.getHref()).request(MediaType.APPLICATION_JSON).get();
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
        List<NetworkType> networks = proxy.deserializeList(response, NetworkType.class);
        int count = 0;
        for (NetworkType network : networks) {
            count++;
            if (count > 10) {
                break;
            }

            // For each Network retrieved we want to read the individual entry.
            response = topology.path("networks/" + network.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            NetworkType networkGet = proxy.deserialize(response, NetworkType.class);
            assertEquals(network.getId(), networkGet.getId());

            // Read the NSA entry.
            response = client.target(networkGet.getNsa().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Read the Services offered by this network.
            for (ResourceRefType service : networkGet.getService()) {
                response = topology.path(service.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            }

            // Read the STP exposed by this network.
            int countStp = 0;
            for (ResourceRefType stp : networkGet.getStp()) {
                response = topology.path(stp.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                countStp++;
                if (countStp > 10) {
                    break;
                }
            }

             // Read the ServiceDomain exposed by this network.
            int countSD = 0;
            for (ResourceRefType serviceDomian : networkGet.getServiceDomain()) {
                response = topology.path(serviceDomian.getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                countSD++;
                if (countSD > 10) {
                    break;
                }
            }
        }
    }

    @Test
    public void testGetServices() throws Exception {
        // Get a list of all STP.
        Response response = topology.path("services").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.
        List<ServiceType> services = proxy.deserializeList(response, ServiceType.class);
        for (ServiceType service : services) {
            // For each Service retrieved we want to read the individual entry.
            response = topology.path(service.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetServiceDomains() throws Exception {
        // Get a list of all ServiceDomains.
        Response response = topology.path("serviceDomains").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.
        List<ServiceDomainType> serviceDomains = proxy.deserializeList(response, ServiceDomainType.class);

        int count = 0;
        for (ServiceDomainType serviceDomain : serviceDomains) {
            // Limit iterations to avoind long builds.
            count++;
            if (count > 10) {
                break;
            }

            // For each ServiceDomain retrieved we want to read the individual entry.
            response = topology.path(serviceDomain.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Now the linked resources.
            ServiceDomainType serviceDomainGet = proxy.deserialize(response, ServiceDomainType.class);
            assertEquals(serviceDomain.getId(), serviceDomainGet.getId());

            response = topology.path(serviceDomainGet.getService().getHref()).request(MediaType.APPLICATION_JSON).get();
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
        List<StpType> stps = proxy.deserializeList(response, StpType.class);
        int count = 0;
        for (StpType stp : stps) {
            count++;
            if (count > 10) {
                break;
            }

            // For each STP retrieved we want to read the individual entry.
            response = topology.path("stps/" + stp.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            StpType stpGet = proxy.deserialize(response, StpType.class);
            assertEquals(stp.getId(), stpGet.getId());

            // Read the direct STP HREF.
            response = topology.path(stpGet.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Now verify the linked resources of this STP exist.
            response = topology.path("networks/" + stp.getNetworkId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(stp.getNetwork().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(stp.getServiceDomain().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            if (stp.getType() == StpDirectionalityType.BIDIRECTIONAL) {
                response = topology.path(stp.getInboundStp().getHref()).request(MediaType.APPLICATION_JSON).get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

                response = topology.path(stp.getOutboundStp().getHref()).request(MediaType.APPLICATION_JSON).get();
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
        List<StpType> stps = proxy.deserializeList(response, StpType.class);
        int filteredStpSize = stps.size();

        // Get all the STP under the same network using a network rooted query.
        response = topology.path("networks/urn:ogf:network:uvalight.net:2013:topology/stps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        stps = proxy.deserializeList(response, StpType.class);
        assertEquals(filteredStpSize, stps.size());
    }

    @Test
    public void testGetSdp() throws Exception {
        // Get a list of all STP.
        Response response = topology.path("sdps").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // We want to run some model consistency checks.  Test at most 10
        // entries otherwise this will take way too long.
        List<SdpType> sdps = proxy.deserializeList(response, SdpType.class);
        int count = 0;
        for (SdpType sdp : sdps) {
            count++;
            if (count > 10) {
                break;
            }

            // For each SDP retrieved we want to read the individual entry.
            response = topology.path("sdps/" + sdp.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            SdpType sdpGet = proxy.deserialize(response, SdpType.class);
            assertEquals(sdp.getId(), sdpGet.getId());

            // Read the direct SDP HREF.
            response = topology.path(sdpGet.getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Now verify the linked resources of this SDP exist.
            response = topology.path(sdp.getDemarcationA().getStp().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(sdp.getDemarcationA().getServiceDomain().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(sdp.getDemarcationA().getNetwork().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(sdp.getDemarcationZ().getStp().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(sdp.getDemarcationZ().getServiceDomain().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            response = topology.path(sdp.getDemarcationZ().getNetwork().getHref()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testLastModified() throws Exception {
        // Get a specific STP.
        Response response = topology.path("stps/urn:ogf:network:netherlight.net:2013:testbed:526?vlan=1784").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Date lastMod = response.getLastModified();

        response = topology.path("stps/urn:ogf:network:netherlight.net:2013:testbed:526?vlan=1784").request(MediaType.APPLICATION_JSON).header("If-Modified-Since", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123)).get();
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());

        // Get a list of all topology resources.
        response = topology.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        CollectionType finalTopology = proxy.deserialize(response, CollectionType.class);
        assertNotNull(finalTopology);

        lastMod = response.getLastModified();

        response = topology.request(MediaType.APPLICATION_JSON).header("If-Modified-Since", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123)).get();
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
    }
}
