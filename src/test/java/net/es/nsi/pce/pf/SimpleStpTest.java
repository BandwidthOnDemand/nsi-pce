package net.es.nsi.pce.pf;

import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class SimpleStpTest {

    public SimpleStpTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
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

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidStpId() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:kddilabs.jp:2013:bi-ps?vlan=1782");
    }

    @Test(expected = IllegalArgumentException.class)
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
