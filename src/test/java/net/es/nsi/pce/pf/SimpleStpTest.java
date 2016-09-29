package net.es.nsi.pce.pf;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.jaxb.path.FindPathErrorType;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class SimpleStpTest {
    private static Logger log;

    public SimpleStpTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        // Configure Log4J and use logs from this point forward.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/log4j.xml"), 45 * 1000);
        log = LoggerFactory.getLogger(SimpleStpTest.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testNetworkId() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013::PMW:ge-2_3_4:+:out?vlan=1999");
        String result = stp.getNetworkId();
        assertEquals("urn:ogf:network:cipo.rnp.br:2013:", result);

        stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1999");
        result = stp.getNetworkId();
        assertEquals("urn:ogf:network:cipo.rnp.br:2013:testbed", result);

        String networkId = SimpleStp.parseNetworkId("urn:ogf:network:icair.org:2013:topology");
        System.out.println(networkId);
        assertEquals("urn:ogf:network:icair.org:2013:topology", networkId);
    }

    @Test
    public void testLocalId() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013::PMW:ge-2_3_4:+:out?vlan=1999");
        String result = stp.getLocalId();
        assertEquals("PMW:ge-2_3_4:+:out", result);

        stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1999");
        result = stp.getLocalId();
        assertEquals("PMW:ge-2_3_4:+:out", result);
    }

    @Test
    public void testLabel() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013::PMW:ge-2_3_4:+:out?vlan=1999");
        Set<SimpleLabel> result = stp.getLabels();
        Set<SimpleLabel> answer = new HashSet<>();
        answer.add(new SimpleLabel("vlan", "1999"));
        assertEquals(answer, result);
    }

    @Test
    public void testLabelRange() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1800-1803");
        Set<SimpleLabel> result = stp.getLabels();
        Set<SimpleLabel> answer = new HashSet<>();
        answer.add(new SimpleLabel("vlan", "1800"));
        answer.add(new SimpleLabel("vlan", "1801"));
        answer.add(new SimpleLabel("vlan", "1802"));
        answer.add(new SimpleLabel("vlan", "1803"));
        assertEquals(answer, result);
    }

    @Test
    public void testLabelTypeFail() {
        try {
            SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan-1800-1803");
        }
        catch (WebApplicationException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            JAXBElement<FindPathErrorType> jaxb = (JAXBElement<FindPathErrorType>) ex.getResponse().getEntity();
            FindPathErrorType error = jaxb.getValue();
            assertEquals(NsiError.UNKNOWN_LABEL_TYPE.getCode(), error.getCode());
            return;
        }

        fail();
    }

    @Test
    public void testLabelRangeFail() {
        try {
            SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1803-1800");
        }
        catch (WebApplicationException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            JAXBElement<FindPathErrorType> jaxb = (JAXBElement<FindPathErrorType>) ex.getResponse().getEntity();
            FindPathErrorType error = jaxb.getValue();
            assertEquals(NsiError.INVALID_LABEL_FORMAT.getCode(), error.getCode());
            return;
        }

        fail();
    }

    @Test
    public void testLabelSequence() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1800,1803,1805");
        Set<SimpleLabel> result = stp.getLabels();
        Set<SimpleLabel> answer = new HashSet<>();
        answer.add(new SimpleLabel("vlan", "1800"));
        answer.add(new SimpleLabel("vlan", "1803"));
        answer.add(new SimpleLabel("vlan", "1805"));
        assertEquals(answer, result);
    }

    @Test
    public void testLabelRangeAndSequence() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1800-1803,1806-1807");
        Set<SimpleLabel> result = stp.getLabels();
        Set<SimpleLabel> answer = new HashSet<>();
        answer.add(new SimpleLabel("vlan", "1800"));
        answer.add(new SimpleLabel("vlan", "1801"));
        answer.add(new SimpleLabel("vlan", "1802"));
        answer.add(new SimpleLabel("vlan", "1803"));
        answer.add(new SimpleLabel("vlan", "1806"));
        answer.add(new SimpleLabel("vlan", "1807"));
        assertEquals(answer, result);
    }

    @Test
    public void testLabelIntersection() {
        SimpleStp stpA = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1800,1803,1805");
        SimpleStp stpZ = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1800-1805");
        boolean result = stpZ.intersectLabels(stpA.getLabels());
        assertTrue(result);
        assertEquals(stpA, stpZ);
    }

    @Test
    public void testStaticCreation() {
        SimpleStp stp1 = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1800-1803,1806-1807");
        Set<SimpleLabel> label = new HashSet<>();
        label.add(new SimpleLabel("vlan", "1800"));
        label.add(new SimpleLabel("vlan", "1801"));
        label.add(new SimpleLabel("vlan", "1802"));
        label.add(new SimpleLabel("vlan", "1803"));
        label.add(new SimpleLabel("vlan", "1806"));
        label.add(new SimpleLabel("vlan", "1807"));
        SimpleStp stp2 = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out", label);
        assertEquals(stp2.getStpId(), stp1.getStpId());
        assertEquals(stp2.getNetworkId(), stp1.getNetworkId());
        assertEquals(stp2.getLocalId(), stp1.getLocalId());
        assertEquals(stp2.getLabels(), stp1.getLabels());
    }

    @Test(expected = WebApplicationException.class)
    public void testInvalidStpId() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:kddilabs.jp:2013:bi-ps?vlan=1782");
    }

    @Test(expected = WebApplicationException.class)
    public void testInvalidLabelRange() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:kddilabs.jp:2013::bi-ps?vlan=1782-1781");
        stp = new SimpleStp("urn:ogf:network:kddilabs.jp:2013::bi-ps?vlan=100-200,1782-1781");
        stp = new SimpleStp("urn:ogf:network:kddilabs.jp:2013::bi-ps?vlan=100,99,200,1782-1786,178-179");
    }

    @Test
    public void testIsUnderSpecified() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:kddilabs.jp:2013::bi-ps?vlan=1782-1790");
        assertTrue(stp.isUnderSpecified());
    }

    @Test
    public void testGetId() {
        String result = SimpleStp.getId("urn:ogf:network:cipo.rnp.br:2013::PMW:ge-2_3_4:+:out?vlan=1999");
        assertEquals("urn:ogf:network:cipo.rnp.br:2013::PMW:ge-2_3_4:+:out", result);

        result = SimpleStp.getId("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1999");
        assertEquals("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out", result);

        result = SimpleStp.getId("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out");
        assertEquals("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out", result);
    }
}
