package net.es.nsi.pce.path;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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

import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.client.TestServer;
import net.es.nsi.pce.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class FindPathEvtsFailedTest {
    private static TestConfig testConfig;
    private static WebTarget target;

    private final static HttpConfig testServer = new HttpConfig() {
        { setUrl("http://localhost:8401/"); setPackageName("net.es.nsi.pce.client"); }
    };

    private final static String callbackURL = testServer.getUrl() + "aggregator/path";

    private final static ObjectFactory factory = new ObjectFactory();

    // First test has mismatched vlans.
    private final static StpTestData test1 = new StpTestData() {
        { this.setStpA("urn:ogf:network:kddilabs.jp:2013:bi-ps?vlan=1782");
          this.setStpZ("urn:ogf:network:uvalight.net:2013:ps?vlan=1781");
        }
    };

    // Second test has unreachable ports.
    private final static StpTestData test2 = new StpTestData() {
        { this.setStpA("urn:ogf:network:czechlight.cesnet.cz:2013:pinger?vlan=1799");
          this.setStpZ("urn:ogf:network:es.net:2013:ps:sunn:1?vlan=1799");
        }
    };

    // Third test has matching vlans but out of range for port.
    private final static StpTestData test3 = new StpTestData() {
        { this.setStpA("urn:ogf:network:aist.go.jp:2013:bi-ps?vlan=4000");
          this.setStpZ("urn:ogf:network:pionier.net.pl:2013:bi-ps?vlan=4000");
        }
    };

    // Fourth test requests a unidirectional STP.
    private final static StpTestData test4 = new StpTestData() {
        { this.setStpA("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:manlan:1?vlan=1779");
          this.setStpZ("urn:ogf:network:manlan.internet2.edu:2013:netherlight:in?vlan=1779");
        }
    };

    // Fifth test request a path with unknown STP.
    private final static StpTestData test5 = new StpTestData() {
        { this.setStpA("urn:ogf:network:aist.go.jp:2013:bi-ps?vlan=1780");
          this.setStpZ("urn:ogf:network:pionier.net.pl:2013:bi-ps-999?vlan=1780");
        }
    };

    private final static List<StpTestData> testData = new ArrayList<StpTestData>() {
        private static final long serialVersionUID = 1L;
        {
            //this.add(test1);
            this.add(test2);
            this.add(test3);
            this.add(test4);
            this.add(test5);
        }
    };

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** FindPathEvtsFailedTest oneTimeSetUp ***********************************");
        // Configure the local test client callback server.
        try {
            TestServer.INSTANCE.start(testServer);
        }
        catch (Exception ex) {
            System.err.println("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
            fail();
        }
        testConfig = new TestConfig();
        target = testConfig.getTarget();
        System.out.println("*************************************** FindPathEvtsFailedTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** FindPathEvtsFailedTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        try {
            TestServer.INSTANCE.shutdown();
        }
        catch (Exception ex) {
            System.err.println("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
            fail();
        }
        System.out.println("*************************************** FindPathEvtsFailedTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testXmlFindPath() throws Exception {
        for (StpTestData test : testData) {
            testPCE(MediaType.APPLICATION_XML, test);
        }
    }

    @Test
    public void testJsonFindPath() throws Exception {
        for (StpTestData test : testData) {
            testPCE(MediaType.APPLICATION_JSON, test);
        }
    }

    @Test
    public void testVersionedXmlFindPath() throws Exception {
        for (StpTestData test : testData) {
            testPCE("application/vnd.net.es.pce.v1+xml", test);
        }
    }

    @Test
    public void testVersionedJsonFindPath() throws Exception {
        for (StpTestData test : testData) {
            testPCE("application/vnd.net.es.pce.v1+json", test);
        }
    }

    public void testPCE(String mediaType, StpTestData test) throws Exception {
        System.out.println("*************************************** testPCE (" + mediaType + ") ***********************************");
        System.out.println("Endpoints: " + test.getStpA() + ", " + test.getStpZ());

        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());

        ReplyToType reply = new ReplyToType();
        reply.setUrl(callbackURL);
        reply.setMediaType(mediaType);
        req.setReplyTo(reply);
        req.setAlgorithm(FindPathAlgorithmType.TREE);

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
        assertEquals(FindPathStatusType.FAILED, findPathResponse.getStatus());
        assertNotNull(findPathResponse.getFindPathError());
        System.out.println(findPathResponse.getFindPathError().getCode() + ":" + findPathResponse.getFindPathError().getLabel());

        System.out.println("*************************************** testPCE done ***********************************");
    }
}
