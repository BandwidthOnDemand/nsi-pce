package net.es.nsi.pce.server;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import net.es.nsi.pce.jersey.RestServer;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.api.jaxb.FindPathRequestType;
import net.es.nsi.pce.api.jaxb.ReplyToType;
import net.es.nsi.pce.jersey.RestClient;

import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.client.TestServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class FindPathSuccessTest extends JerseyTest {

    private final static HttpConfig testServer = new HttpConfig() {
        { url = "http://localhost:9800/"; packageName = "net.es.nsi.pce.client"; }
    };
    
    private final static String callbackURL = testServer.url + "aggregator/path";
    
    private final static ObjectFactory factory = new ObjectFactory();
    
    private final static StpTestData test1 = new StpTestData() {
        { this.getStpA().setLocalId("urn:ogf:network:kddilabs.jp:2013:bi-ps");
          this.getStpA().setNetworkId("urn:ogf:network:kddilabs.jp:2013:topology");
          this.setVlanA(1781);
          this.getStpZ().setLocalId("urn:ogf:network:uvalight.net:2013:bi-ps");
          this.getStpZ().setNetworkId("urn:ogf:network:uvalight.net:2013:topology");
          this.setVlanZ(1781);
        }
    };

    private final static StpTestData test2 = new StpTestData() {
        { this.getStpA().setLocalId("urn:ogf:network:uvalight.net:2013:bi-ps");
          this.getStpA().setNetworkId("urn:ogf:network:uvalight.net:2013:topology");
          this.setVlanA(1780);
          this.getStpZ().setLocalId("urn:ogf:network:es.net:2013:ps:sunn:1");
          this.getStpZ().setNetworkId("urn:ogf:network:es.net:2013");
          this.setVlanZ(1780);
        }
    };

    private final static StpTestData test3 = new StpTestData() {
        { this.getStpA().setLocalId("urn:ogf:network:aist.go.jp:2013:bi-ps");
          this.getStpA().setNetworkId("urn:ogf:network:aist.go.jp:2013:topology");
          this.setVlanA(1780);
          this.getStpZ().setLocalId("urn:ogf:network:pionier.net.pl:2013:bi-ps");
          this.getStpZ().setNetworkId("urn:ogf:network:pionier.net.pl:2013:topology");
          this.setVlanZ(1780);
        }
    };

    private final static StpTestData test4 = new StpTestData() {
        { this.getStpA().setLocalId("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:241");
          this.getStpA().setNetworkId("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
          this.setVlanA(1799);
          this.getStpZ().setLocalId("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:232");
          this.getStpZ().setNetworkId("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
          this.setVlanZ(1799);
        }
    };

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        
        // Configure the local test client callback server.
        TestServer.INSTANCE.start(testServer);
        
        // Configure test instance of PCE server.
        try {
            ConfigurationManager.INSTANCE.initialize("src/test/resources/config/");
        } catch (Exception ex) {
            System.err.println("configure(): Could not initialize test environment." + ex.toString());
            fail("configure(): Could not initialize test environment.");
        }
        
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().packageName);
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }

    @Test
    public void testXmlFindPath() throws Exception {
        testSuccessfulPath(MediaType.APPLICATION_XML, test1);
        testSuccessfulPath(MediaType.APPLICATION_XML, test2);
        testSuccessfulPath(MediaType.APPLICATION_XML, test3);
        testSuccessfulPath(MediaType.APPLICATION_XML, test4);
    }
    
    @Test
    public void testJsonFindPath() throws Exception {
        testSuccessfulPath(MediaType.APPLICATION_JSON, test1);
        testSuccessfulPath(MediaType.APPLICATION_JSON, test2);
        testSuccessfulPath(MediaType.APPLICATION_JSON, test3);
        testSuccessfulPath(MediaType.APPLICATION_JSON, test4);
    }
    
    @Test
    public void testVersionedXmlFindPath() throws Exception {
        testSuccessfulPath("application/vnd.net.es.pce.v1+xml", test1);
        testSuccessfulPath("application/vnd.net.es.pce.v1+xml", test2);
        testSuccessfulPath("application/vnd.net.es.pce.v1+xml", test3);
        testSuccessfulPath("application/vnd.net.es.pce.v1+xml", test4);
    }

    @Test
    public void testVersionedJsonFindPath() throws Exception {
        testSuccessfulPath("application/vnd.net.es.pce.v1+json", test1);
        testSuccessfulPath("application/vnd.net.es.pce.v1+json", test2);
        testSuccessfulPath("application/vnd.net.es.pce.v1+json", test3);
        testSuccessfulPath("application/vnd.net.es.pce.v1+json", test4);
    }
        
    public void testSuccessfulPath(String mediaType, StpTestData test) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());
        
        ReplyToType reply = new ReplyToType();
        reply.setUrl(callbackURL);
        reply.setMediaType(mediaType);
        req.setReplyTo(reply);
        req.setAlgorithm(FindPathAlgorithmType.CHAIN);
                
        // Reservation start time is 2 minutes from now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        req.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));
 
        // Reservation end time is 12 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 12);
        req.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime));
        
        req.setServiceType("http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE");

        // We want an EVTS service for this test.
        EthernetVlanType evts = new EthernetVlanType();
        evts.setCapacity(100L);
        evts.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        evts.setSymmetricPath(Boolean.TRUE);
        
        // Format the source STP.
        evts.setSourceSTP(test.getStpA());
        evts.setSourceVLAN(test.getVlanA());
        
        // Format the destination STP.
        evts.setDestSTP(test.getStpZ());
        evts.setDestVLAN(test.getVlanZ());

        req.getAny().add(factory.createEvts(evts));

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }
}
