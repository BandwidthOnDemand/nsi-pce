/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import java.util.HashMap;
import java.util.Map;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class StpTypeBundleTest {
    private static Logger log;
    private static final ObjectFactory factory = new ObjectFactory();
    private NsiTopology mockedTopology;

    @BeforeClass
    public static void setUpClass() throws Exception {
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/log4j.xml"), 45 * 1000);
        log = LoggerFactory.getLogger(DijkstraPCETest.class);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        buildMockedTopology();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConstructor() {
        System.out.println("testConstructor");
        SimpleStp stp = new SimpleStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1900-1904");
        assertEquals(5, stp.getMemberStpId().size());
        StpTypeBundle bundle = new StpTypeBundle(mockedTopology, stp, DirectionalityType.BIDIRECTIONAL);
        assertTrue(stp.getMemberStpId().containsAll(bundle.keySet()));
        assertTrue(bundle.keySet().containsAll(stp.getMemberStpId()));

        stp = new SimpleStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out?vlan=1701-1702");
        assertEquals(2, stp.getMemberStpId().size());
        bundle = new StpTypeBundle(mockedTopology, stp, DirectionalityType.BIDIRECTIONAL);
        assertTrue(stp.getMemberStpId().containsAll(bundle.keySet()));
        assertTrue(bundle.keySet().containsAll(stp.getMemberStpId()));

        stp = new SimpleStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1800-1801");
        assertEquals(2, stp.getMemberStpId().size());
        bundle = new StpTypeBundle(mockedTopology, stp, DirectionalityType.BIDIRECTIONAL);
        assertTrue(bundle.isEmpty());
    }

    private void buildMockedTopology() {
        mockedTopology = mock(NsiTopology.class);

        // Build STP in the source domain.
        StpType stp1 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1900", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:start");
        StpType stp2 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1901", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:start");
        StpType stp3 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1902", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:start");
        StpType stp4 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1903", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:start");
        StpType stp5 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start?vlan=1904", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:start");

        StpType stp6 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out?vlan=1700", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");
        StpType stp7 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out?vlan=1701", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");
        StpType stp8 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out?vlan=1702", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");
        StpType stp9 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out?vlan=1703", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");
        StpType stp10 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out?vlan=1704", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");

        final Map<String, StpType> stps = new HashMap<>();
        stps.put(stp1.getId(), stp1);
        stps.put(stp2.getId(), stp2);
        stps.put(stp3.getId(), stp3);
        stps.put(stp4.getId(), stp4);
        stps.put(stp5.getId(), stp5);
        stps.put(stp6.getId(), stp6);
        stps.put(stp7.getId(), stp7);
        stps.put(stp8.getId(), stp8);
        stps.put(stp9.getId(), stp9);
        stps.put(stp10.getId(), stp10);

        // mock for method for getServiceDomains
        when(mockedTopology.getLocalNsaId()).thenReturn("urn:ogf:network:surfnet.nl:1990:nsa");

        // mock for method getStp
        when(mockedTopology.getStp(anyString())).thenAnswer(new Answer<StpType>() {
            @Override
            public StpType answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    String key = (String) arguments[0];
                    if (stps.containsKey(key)) {
                        return stps.get(key);
                    }
                }
                return null;
            }
        });

    }

    private StpType createStp(String id, String networkId, String localId) {
        StpType stp = new StpType();
        stp.setNetworkId(networkId);
        stp.setLocalId(localId);
        stp.setId(id);
        stp.setType(StpDirectionalityType.BIDIRECTIONAL);
        ResourceRefType ref = factory.createResourceRefType();
        ref.setId(id);
        stp.setSelf(ref);
        return stp;
    }

}
