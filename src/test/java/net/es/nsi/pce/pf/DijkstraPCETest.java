package net.es.nsi.pce.pf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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

public class DijkstraPCETest {
    private static Logger log;
    private static final ObjectFactory factory = new ObjectFactory();
    private NsiTopology mockedTopology;
    private List<GraphEdge> path;

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
    public void testPullIndividualSegmentsOut() {
        log.debug("testPullIndividualSegmentsOut");
        // given
        DijkstraPCE subject = new DijkstraPCE();

        // when
        List<StpPair> segments = subject.pullIndividualSegmentsOut(path, mockedTopology);

        // then
        assertThat(segments.size(), is(3));
        assertThat(segments.get(0).getA().getId(), is("urn:ogf:network:surfnet.nl:1990:src-testbed:start"));
        assertThat(segments.get(2).getZ().getId(), is("urn:ogf:network:surfnet.nl:1990:dst-testbed:end"));
    }

    /**
     * Test of apply method, of class DijkstraPCE.
     * 
     * @throws javax.xml.datatype.DatatypeConfigurationException
     */
    @Test
    public void testApply() throws DatatypeConfigurationException {
        log.debug("testApply");

        // Build the reservation contraint list.
        Set<Constraint> constraints = new HashSet<>();

        StringAttrConstraint sourceStp = new StringAttrConstraint();
        sourceStp.setAttrName(Point2PointTypes.SOURCESTP);
        sourceStp.setValue("urn:ogf:network:surfnet.nl:1990:src-testbed:start");
        constraints.add(sourceStp);

        StringAttrConstraint destStp = new StringAttrConstraint();
        destStp.setAttrName(Point2PointTypes.DESTSTP);
        destStp.setValue("urn:ogf:network:surfnet.nl:1990:dst-testbed:end");
        constraints.add(destStp);

        Set<Constraint> scheduleConstraints = PCEConstraints.getConstraints(
                XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()),
                XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 1000*360),
                NsiConstants.EVTS_AGOLE, null);

        constraints.addAll(scheduleConstraints);

        PCEData pceData = new PCEData();
        pceData.addConstraints(constraints);
        pceData.setTopology(mockedTopology);

        DijkstraPCE subect = new DijkstraPCE();
        PCEData result = subect.apply(pceData);
        assertEquals(result.getPath().getPathSegments().size(), 3);
    }

    private void buildMockedTopology() {
        mockedTopology = mock(NsiTopology.class);

        // Create service domains for all three domains.
        ServiceDomainType srcServiceDomain = factory.createServiceDomainType();
        srcServiceDomain.setId("urn:ogf:network:surfnet.nl:1990:src-testbed:ServiceDomain");
        ResourceRefType srcRef = factory.createResourceRefType();
        srcRef.setId(srcServiceDomain.getId());
        srcRef.setType(NsiConstants.EVTS_AGOLE);
        srcServiceDomain.setSelf(srcRef);

        ServiceDomainType intServiceDomain = factory.createServiceDomainType();
        intServiceDomain.setId("urn:ogf:network:surfnet.nl:1990:inter-testbed:ServiceDomain");
        ResourceRefType intRef = factory.createResourceRefType();
        intRef.setId(intServiceDomain.getId());
        intRef.setType(NsiConstants.EVTS_AGOLE);
        intServiceDomain.setSelf(intRef);

        ServiceDomainType dstServiceDomain = factory.createServiceDomainType();
        dstServiceDomain.setId("urn:ogf:network:surfnet.nl:1990:dst-testbed:ServiceDomain");
        ResourceRefType dstRef = factory.createResourceRefType();
        dstRef.setId(dstServiceDomain.getId());
        dstRef.setType(NsiConstants.EVTS_AGOLE);
        dstServiceDomain.setSelf(dstRef);

        // Build STP in the source domain.
        StpType srcStp1 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:start", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:start");
        srcStp1.setServiceDomain(srcRef);

        StpEdge srcEdge = new StpEdge(srcStp1.getId(), srcStp1, srcServiceDomain);

        StpType srcStp2 = createStp("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out", "urn:ogf:network:surfnet.nl:1990:src-testbed", "urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");
        srcStp2.setServiceDomain(srcRef);

        // Build STP in intermediate domain.
        StpType intermediateStp1 = createStp("urn:ogf:network:surfnet.nl:1990:inter-testbed:in", "urn:ogf:network:surfnet.nl:1990:inter-testbed", "urn:ogf:network:surfnet.nl:1990:inter-testbed:in");
        intermediateStp1.setServiceDomain(intRef);

        SdpType interSdp1 = createSdp(srcStp2, intermediateStp1);
        interSdp1.setType(SdpDirectionalityType.BIDIRECTIONAL);
        SdpEdge interEdge1 = new SdpEdge(interSdp1.getId(), interSdp1);

        StpType intermediateStp2 = createStp("urn:ogf:network:surfnet.nl:1990:inter-testbed:out", "urn:ogf:network:surfnet.nl:1990:inter-testbed", "urn:ogf:network:surfnet.nl:1990:inter-testbed:out");
        intermediateStp2.setServiceDomain(intRef);

        // Build STP in destimation domain.
        StpType dstStp2 = createStp("urn:ogf:network:surfnet.nl:1990:dst-testbed:stp-in", "urn:ogf:network:surfnet.nl:1990:dst-testbed", "urn:ogf:network:surfnet.nl:1990:dst-testbed:stp-in");
        dstStp2.setServiceDomain(dstRef);

        SdpType interSdp2 = createSdp(intermediateStp2, dstStp2);
        interSdp2.setType(SdpDirectionalityType.BIDIRECTIONAL);
        SdpEdge interEdge2 = new SdpEdge(interSdp2.getId(), interSdp2);

        StpType dstStp1 = createStp("urn:ogf:network:surfnet.nl:1990:dst-testbed:end", "urn:ogf:network:surfnet.nl:1990:dst-testbed", "urn:ogf:network:surfnet.nl:1990:dst-testbed:end");
        dstStp1.setServiceDomain(dstRef);

        StpEdge dstEdge = new StpEdge(dstStp1.getId(), dstStp1, dstServiceDomain);

        path = Arrays.asList(srcEdge, interEdge1, interEdge2, dstEdge);

        final Map<String, StpType> stps = new HashMap<>();
        stps.put(srcStp1.getId(), srcStp1);
        stps.put(srcStp2.getId(), srcStp2);
        stps.put(dstStp1.getId(), dstStp1);
        stps.put(dstStp2.getId(), dstStp2);
        stps.put(intermediateStp1.getId(), intermediateStp1);
        stps.put(intermediateStp2.getId(), intermediateStp2);

        final Map<String, ServiceDomainType> serviceDomains = new HashMap<>();
        serviceDomains.put(srcServiceDomain.getId(), srcServiceDomain);
        serviceDomains.put(intServiceDomain.getId(), intServiceDomain);
        serviceDomains.put(dstServiceDomain.getId(), dstServiceDomain);

        final Map<String, SdpType> sdps = new HashMap<>();
        sdps.put(interSdp1.getId(), interSdp1);
        sdps.put(interSdp2.getId(), interSdp2);

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

        // mock for method for getServiceDomain
        when(mockedTopology.getServiceDomain(anyString())).thenAnswer(new Answer<ServiceDomainType>() {
            @Override
            public ServiceDomainType answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    String key = (String) arguments[0];
                    if (serviceDomains.containsKey(key)) {
                        return serviceDomains.get(key);
                    }
                }
                return null;
            }
        });

        // mock for method for getServiceDomains
        when(mockedTopology.getServiceDomains()).thenReturn(serviceDomains.values());

        // mock for method for getServiceDomains
        when(mockedTopology.getSdps()).thenReturn(sdps.values());


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

    private SdpType createSdp(StpType start, StpType end) {
        SdpType edge = new SdpType();
        edge.setId(start.getId() + "::" + end.getId());

        DemarcationType demarcationA = factory.createDemarcationType();
        demarcationA.setStp(start.getSelf());
        demarcationA.setServiceDomain(start.getServiceDomain());

        DemarcationType demarcationB = factory.createDemarcationType();
        demarcationB.setStp(end.getSelf());
        demarcationB.setServiceDomain(end.getServiceDomain());

        edge.setDemarcationA(demarcationA);
        edge.setDemarcationZ(demarcationB);

        return edge;
    }
}
