package net.es.nsi.pce.path;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
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
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.path.jaxb.FindPathRequestType;
import net.es.nsi.pce.path.jaxb.FindPathResponseType;
import net.es.nsi.pce.path.jaxb.FindPathStatusType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.jaxb.ReplyToType;
import net.es.nsi.pce.jersey.RestClient;

import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.client.TestServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

public class FindPathEvtsSuccessTest extends JerseyTest {

    private final static HttpConfig testServer = new HttpConfig() {
        { setUrl("http://localhost:9801/"); setPackageName("net.es.nsi.pce.client"); }
    };
    
    private final static String callbackURL = testServer.getUrl() + "aggregator/path";
    
    private final static ObjectFactory factory = new ObjectFactory();
    
    private final static StpTestData test1 = new StpTestData() {
        { this.setStpA("urn:ogf:network:kddilabs.jp:2013:bi-ps?vlan=1782");
          this.setStpZ("urn:ogf:network:uvalight.net:2013:ps?vlan=1782");
        }
    };

    private final static StpTestData test2 = new StpTestData() {
        { this.setStpA("urn:ogf:network:uvalight.net:2013:ps?vlan=1780");
          this.setStpZ("urn:ogf:network:es.net:2013:ps:sunn:1?vlan=1780");
        }
    };

    private final static StpTestData test3 = new StpTestData() {
        { this.setStpA("urn:ogf:network:aist.go.jp:2013:bi-ps?vlan=1780");
          this.setStpZ("urn:ogf:network:pionier.net.pl:2013:bi-ps?vlan=1780");
        }
    };

    private final static StpTestData test4 = new StpTestData() {
        { this.setStpA("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:241?vlan=1799");
          this.setStpZ("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:232?vlan=1799");
        }
    };
    
    // Fifth test request two STP on either end of an SDP.
    private final static StpTestData test5 = new StpTestData() {
        { this.setStpA("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:manlan:1?vlan=1799");
          this.setStpZ("urn:ogf:network:manlan.internet2.edu:2013:netherlight?vlan=1799");
        }
    };
    
    // Netherlight endpoints.
    private final static StpTestData test6 = new StpTestData() {
        { this.setStpA("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:282?vlan=1784");
          this.setStpZ("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:manlan:1?vlan=1784");
        }
    };
    
    private final static List<StpTestData> testData = new ArrayList<StpTestData>() {
        private static final long serialVersionUID = 1L;
        {
            this.add(test1);
            this.add(test2);
            this.add(test3);
            this.add(test4);
            this.add(test5);
            this.add(test6);
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
        
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().getPackageName());
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }

    @Test
    public void testXmlFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath(MediaType.APPLICATION_XML, test);
        }
    }
    
    @Test
    public void testJsonFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath(MediaType.APPLICATION_JSON, test);
        }
    }
    
    @Test
    public void testVersionedXmlFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath("application/vnd.net.es.pce.v1+xml", test);
        }
    }

    @Test
    public void testVersionedJsonFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath("application/vnd.net.es.pce.v1+json", test);
        }
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
        
        req.setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");

        // We want an P2PS service element for this test.
        P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);
        
        // Format the source STP.
        p2ps.setSourceSTP(test.getStpA());
        
        // Format the destination STP.
        p2ps.setDestSTP(test.getStpZ());

        req.getAny().add(factory.createP2Ps(p2ps));

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        // Reset our results for this run.
        TestServer.INSTANCE.setFindPathResponse(null);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        
        FindPathResponseType findPathResponse = TestServer.INSTANCE.getFindPathResponse();
        int count = 0;
        while(findPathResponse == null && count < 30) {
            count++;
            Thread.sleep(1000);
        }
        
        assertNotNull(findPathResponse);
        
        assertEquals(FindPathStatusType.SUCCESS, findPathResponse.getStatus());
    }
}