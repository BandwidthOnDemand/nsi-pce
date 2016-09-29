package net.es.nsi.pce.path;

import com.google.common.collect.Lists;
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
import net.es.nsi.pce.client.TestServer;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.FindPathAlgorithmType;
import net.es.nsi.pce.jaxb.path.FindPathRequestType;
import net.es.nsi.pce.jaxb.path.FindPathResponseType;
import net.es.nsi.pce.jaxb.path.FindPathStatusType;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.path.ReplyToType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.jaxb.path.TraceType;
import net.es.nsi.pce.jaxb.path.TypeValueType;
import net.es.nsi.pce.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class FindPathEvtsSuccessTest {
    private static TestConfig testConfig;
    private static WebTarget target;

    private final static HttpConfig TEST_SERVER = new HttpConfig() {
        { setUrl("http://localhost:8401/"); setPackageName("net.es.nsi.pce.client"); }
    };

    private final static String CALLBACK_URL = TEST_SERVER.getUrl() + "aggregator/path";

    private final static ObjectFactory FACTORY = new ObjectFactory();

        // urn:ogf:network:netherlight.net:2013:production7:starlight-1?vlan=1782, stpZ=urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1782

    private final static StpTestData test0 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:netherlight.net:2013:production7:starlight-1?vlan=1782");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1782");
        }
    };

    private final static StpTestData test1 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1782");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1782");
            StpListType ero = FACTORY.createStpListType();
            OrderedStpType interdomain = FACTORY.createOrderedStpType();
            interdomain.setOrder(0);
            interdomain.setStp("urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1780-1790");
            ero.getOrderedSTP().add(interdomain);
            this.setEro(ero);
        }
    };

    private final static StpTestData test2 = new StpTestData() {
        { this.setStpA("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1780");
          this.setStpZ("urn:ogf:network:es.net:2013::sunn-cr5:10_1_6:+?vlan=1780");
        }
    };

    private final static StpTestData test3 = new StpTestData() {
        { this.setStpA("urn:ogf:network:aist.go.jp:2013:topology:bi-ps?vlan=1780");
          this.setStpZ("urn:ogf:network:pionier.net.pl:2013:topology:server_port?vlan=1780");
        }
    };

    // Label swapping in single domain.
    private final static StpTestData test4 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:netherlight.net:2013:production7:surfnet-1?vlan=1700");
            this.setStpZ("urn:ogf:network:netherlight.net:2013:production7:geant-1?vlan=2102");
        }
    };

    // Two endpoint in same domain.
    private final static StpTestData test5 = new StpTestData() {
        { this.setStpA("urn:ogf:network:netherlight.net:2013:production7:nordunet-1?vlan=1779");
          this.setStpZ("urn:ogf:network:netherlight.net:2013:production7:czechlight-1?vlan=1784");
        }
    };

    private final static StpTestData test6 = new StpTestData() {
        { this.setStpA("urn:ogf:network:es.net:2013::aofa-cr5:4_2_1:+?vlan=3901");
          this.setStpZ("urn:ogf:network:surfnet.nl:1990:production7:96292?vlan=3901");
        }
    };

    // Two underspecified STP.
    private final static StpTestData test7 = new StpTestData() {
        { this.setStpA("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1780-1785");
          this.setStpZ("urn:ogf:network:es.net:2013::sunn-cr5:10_1_6:+?vlan=1780-1785");
        }
    };


    // Two underspecified STP.
    private final static StpTestData test7b = new StpTestData() {
        { this.setStpA("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1780-1785");
          this.setStpZ("urn:ogf:network:es.net:2013::sunn-cr5:10_1_6:+?vlan=1780-1785");
            StpListType ero = FACTORY.createStpListType();
            OrderedStpType intermediate = FACTORY.createOrderedStpType();
            intermediate.setOrder(0);
            intermediate.setStp("urn:ogf:network:netherlight.net:2013:production7:esnet-1?vlan=1005-1010");
            ero.getOrderedSTP().add(intermediate);
            this.setEro(ero);
        }
    };

    // One underspecified STP.
    private final static StpTestData test8 = new StpTestData() {
        { this.setStpA("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1780");
          this.setStpZ("urn:ogf:network:es.net:2013::sunn-cr5:10_1_6:+?vlan=1780-1785");
        }
    };

    // Underpecified STP single domain.
    private final static StpTestData test9 = new StpTestData() {
        { this.setStpA("urn:ogf:network:netherlight.net:2013:production7:surfnet-1?vlan=1700-1720");
          this.setStpZ("urn:ogf:network:netherlight.net:2013:production7:geant-1?vlan=2100-2102");
        }
    };

    // Single internal STP in ERO.
    private final static StpTestData test10 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:netherlight.net:2013:production7:surfnet-1?vlan=1700");
            this.setStpZ("urn:ogf:network:netherlight.net:2013:production7:geant-1?vlan=2102");
            StpListType ero = FACTORY.createStpListType();
            OrderedStpType internal1 = FACTORY.createOrderedStpType();
            internal1.setOrder(0);
            internal1.setStp("urn:ogf:network:netherlight.net:2013:production7:internal1");
            ero.getOrderedSTP().add(internal1);
            this.setEro(ero);
        }
    };

    // Two internal STP in ERO.
    private final static StpTestData test11 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:netherlight.net:2013:production7:surfnet-1?vlan=1700");
            this.setStpZ("urn:ogf:network:netherlight.net:2013:production7:geant-1?vlan=2102");
            StpListType ero = FACTORY.createStpListType();
            OrderedStpType internal1 = FACTORY.createOrderedStpType();
            internal1.setOrder(0);
            internal1.setStp("urn:ogf:network:netherlight.net:2013:production7:internal1");
            ero.getOrderedSTP().add(internal1);
            OrderedStpType internal2 = FACTORY.createOrderedStpType();
            internal2.setOrder(1);
            internal2.setStp("urn:ogf:network:netherlight.net:2013:production7:internal2");
            ero.getOrderedSTP().add(internal2);
            this.setEro(ero);
        }
    };

    // Two internal STP in ERO plus an external.
    private final static StpTestData test12 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1782");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1782");
            StpListType ero = FACTORY.createStpListType();

            OrderedStpType internal1 = FACTORY.createOrderedStpType();
            internal1.setOrder(1);
            internal1.setStp("urn:ogf:network:kddilabs.jp:2013:topology:internal1");
            ero.getOrderedSTP().add(internal1);

            OrderedStpType internal2 = FACTORY.createOrderedStpType();
            internal2.setOrder(2);
            internal2.setStp("urn:ogf:network:kddilabs.jp:2013:topology:internal2");
            ero.getOrderedSTP().add(internal2);

            OrderedStpType edge = FACTORY.createOrderedStpType();
            edge.setOrder(3);
            edge.setStp("urn:ogf:network:kddilabs.jp:2013:topology:bi-kddilabs-jgn-x?vlan=1782");
            ero.getOrderedSTP().add(edge);

            OrderedStpType interdomain = FACTORY.createOrderedStpType();
            interdomain.setOrder(3);
            interdomain.setStp("urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1780-1790");
            ero.getOrderedSTP().add(interdomain);
            this.setEro(ero);
        }
    };

    // Two internal STP in each of src and dst network plus an external.
    private final static StpTestData test13 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1782");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1782");
            StpListType ero = FACTORY.createStpListType();

            OrderedStpType internal1 = FACTORY.createOrderedStpType();
            internal1.setOrder(0);
            internal1.setStp("urn:ogf:network:kddilabs.jp:2013:topology:internalA");
            ero.getOrderedSTP().add(internal1);

            OrderedStpType internal2 = FACTORY.createOrderedStpType();
            internal2.setOrder(1);
            internal2.setStp("urn:ogf:network:kddilabs.jp:2013:topology:internalB");
            ero.getOrderedSTP().add(internal2);

            OrderedStpType edge1 = FACTORY.createOrderedStpType();
            edge1.setOrder(2);
            edge1.setStp("urn:ogf:network:kddilabs.jp:2013:topology:bi-kddilabs-jgn-x?vlan=1782");
            ero.getOrderedSTP().add(edge1);

            OrderedStpType interdomain = FACTORY.createOrderedStpType();
            interdomain.setOrder(3);
            interdomain.setStp("urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1780-1790");
            ero.getOrderedSTP().add(interdomain);

            // We should allow this to be urn:ogf:network:uvalight.net:2013:topology:netherlight?vlan=1782.
            OrderedStpType edge2 = FACTORY.createOrderedStpType();
            edge2.setOrder(4);
            edge2.setStp("urn:ogf:network:netherlight.net:2013:production7:uva-3?vlan=1782");
            ero.getOrderedSTP().add(edge2);

            OrderedStpType internal3 = FACTORY.createOrderedStpType();
            internal3.setOrder(5);
            internal3.setStp("urn:ogf:network:uvalight.net:2013:topology:internalA");
            ero.getOrderedSTP().add(internal3);

            OrderedStpType internal4 = FACTORY.createOrderedStpType();
            internal4.setOrder(6);
            internal4.setStp("urn:ogf:network:uvalight.net:2013:topology:internalB");
            ero.getOrderedSTP().add(internal4);
            this.setEro(ero);
        }
    };

    // Two internal STP in each of src and dst network plus an external.
    private final static StpTestData test14 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1782");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1782");
            StpListType ero = FACTORY.createStpListType();

            OrderedStpType internal1 = FACTORY.createOrderedStpType();
            internal1.setOrder(0);
            internal1.setStp("urn:ogf:network:netherlight.net:2013:production7:internalA");
            ero.getOrderedSTP().add(internal1);

            // We should allow this to be urn:ogf:network:uvalight.net:2013:topology:netherlight?vlan=1782.
            OrderedStpType internal2 = FACTORY.createOrderedStpType();
            internal2.setOrder(1);
            internal2.setStp("urn:ogf:network:netherlight.net:2013:production7:internalB");
            ero.getOrderedSTP().add(internal2);
            this.setEro(ero);
        }
    };

    private final static StpTestData test15 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1780-1790");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1780-1790");

            StpListType ero = FACTORY.createStpListType();
            OrderedStpType intermediate = FACTORY.createOrderedStpType();
            intermediate.setOrder(0);
            intermediate.setStp("urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1780-1790");
            ero.getOrderedSTP().add(intermediate);
            this.setEro(ero);
        }
    };

    private final static StpTestData test16 = new StpTestData() {
        {   this.setStpA("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1780-1790");
            this.setStpZ("urn:ogf:network:uvalight.net:2013:topology:ps?vlan=1780-1790");

            StpListType ero = FACTORY.createStpListType();
            OrderedStpType intermediate = FACTORY.createOrderedStpType();
            intermediate.setOrder(0);
            intermediate.setStp("urn:ogf:network:icair.org:2013:topology");
            ero.getOrderedSTP().add(intermediate);
            this.setEro(ero);
        }
    };

    private final static List<StpTestData> testData = new ArrayList<StpTestData>() {
        private static final long serialVersionUID = 1L;
        {
            this.add(test0);
            this.add(test1);
            this.add(test2);
            this.add(test3);
            this.add(test4);
            this.add(test5);
            this.add(test6);
            this.add(test7);
            this.add(test7b);
            this.add(test8);
            this.add(test9);
            this.add(test10);
            this.add(test11);
            this.add(test12);
            this.add(test13);
            this.add(test14);
            this.add(test15);
            /*this.add(test16);*/
        }
    };

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** FindPathEvtsSuccessTest oneTimeSetUp ***********************************");
        // Configure the local test client callback server.
        try {
            TestServer.INSTANCE.start(TEST_SERVER);
        }
        catch (Exception ex) {
            System.err.println("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
            fail();
        }

        testConfig = new TestConfig();
        target = testConfig.getTarget();
        System.out.println("*************************************** FindPathEvtsSuccessTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** FindPathEvtsSuccessTest oneTimeTearDown ***********************************");
        System.out.println("@After - tearDown");
        testConfig.shutdown();
        try {
            TestServer.INSTANCE.shutdown();
        }
        catch (Exception ex) {
            System.err.println("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
            fail();
        }
        System.out.println("*************************************** FindPathEvtsSuccessTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testXmlFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath(MediaType.APPLICATION_XML, test, FindPathAlgorithmType.TREE);
        }
    }

    @Test
    public void testJsonFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath(MediaType.APPLICATION_JSON, test, FindPathAlgorithmType.TREE);
        }
    }

    @Test
    public void testVersionedXmlFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath("application/vnd.net.es.pce.v1+xml", test, FindPathAlgorithmType.TREE);
        }
    }

    @Test
    public void testVersionedJsonFindPath() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath("application/vnd.net.es.pce.v1+json", test, FindPathAlgorithmType.TREE);
        }
    }

    @Test
    public void testXmlFindPathWithSequential() throws Exception {
        for (StpTestData test : testData) {
            testSuccessfulPath(MediaType.APPLICATION_XML, test, FindPathAlgorithmType.SEQUENTIAL);
        }
    }

    public void testSuccessfulPath(String mediaType, StpTestData test, FindPathAlgorithmType algorithm) throws Exception {
        System.out.println("*************************************** testSuccessfulPath(" + mediaType + ") ***********************************");
        System.out.println("Endpoints: " + test.getStpA() + ", " + test.getStpZ());

        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());

        ReplyToType reply = new ReplyToType();
        reply.setUrl(CALLBACK_URL);
        reply.setMediaType(mediaType);
        req.setReplyTo(reply);
        req.setAlgorithm(algorithm);

        // Reservation start time is 2 minutes from now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        req.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));

        // Reservation end time is 12 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 12);
        req.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime));

        req.setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");

        List<TraceType> trace = Lists.newArrayList(
                traceType(2, "urn:ogf:network:surfnet.nl:1990:nsa:bod-acc"),
                traceType(5, "urn:ogf:network:sinet.ac.jp:2013:nsa"),
                traceType(3, "urn:ogf:network:aist.go.jp:2013:nsa"),
                traceType(4, "urn:ogf:network:pionier.net.pl:2013:nsa"),
                traceType(1, "urn:ogf:network:geant.net:2013:nsa")
            );

        req.getTrace().addAll(trace);

        // We want an P2PS service element for this test.
        P2PServiceBaseType p2ps = FACTORY.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);

        // Format the source STP.
        p2ps.setSourceSTP(test.getStpA());

        // Format the destination STP.
        p2ps.setDestSTP(test.getStpZ());

        // Add the ero.
        p2ps.setEro(test.getEro());

        TypeValueType tvt = FACTORY.createTypeValueType();
        tvt.setType("Poopies");
        tvt.setValue("Doodies");
        p2ps.getParameter().add(tvt);

        req.getAny().add(FACTORY.createP2Ps(p2ps));

        JAXBElement<FindPathRequestType> jaxbRequest = FACTORY.createFindPathRequest(req);

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

        System.out.println("*************************************** testSuccessfulPath done ***********************************");
    }

    private TraceType traceType(int index, String value) {
        TraceType trace = FACTORY.createTraceType();
        trace.setIndex(index);
        trace.setValue(value);
        return trace;
    }
}
