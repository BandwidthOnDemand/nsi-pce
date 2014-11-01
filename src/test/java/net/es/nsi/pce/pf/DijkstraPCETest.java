package net.es.nsi.pce.pf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.is;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DijkstraPCETest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void should_pull_out_individual_segments() {
        // given
        DijkstraPCE subject = new DijkstraPCE();
        NsiTopology mockedTopology = mock(NsiTopology.class);

        StpType srcStp1 = new StpType();
        srcStp1.setNetworkId("urn:ogf:network:surfnet.nl:1990:src-testbed");
        srcStp1.setId("urn:ogf:network:surfnet.nl:1990:src-testbed:start");

        ServiceDomainType srcServiceDomain = mock(ServiceDomainType.class);
        srcServiceDomain.setId("urn:ogf:network:surfnet.nl:1990:src-testbed:ServiceDomain");

        StpEdge srcEdge = new StpEdge(srcStp1.getId(), srcStp1, srcServiceDomain);

        StpType srcStp2 = new StpType();
        srcStp2.setNetworkId("urn:ogf:network:surfnet.nl:1990:src-testbed");
        srcStp2.setId("urn:ogf:network:surfnet.nl:1990:src-testbed:stp-out");

        StpType intermediateStp1 = new StpType();
        intermediateStp1.setNetworkId("urn:ogf:network:surfnet.nl:1990:inter-testbed");
        intermediateStp1.setId("urn:ogf:network:surfnet.nl:1990:inter-testbed:in");

        SdpType interSdp1 = createSdp(srcStp2, intermediateStp1);
        SdpEdge interEdge1 = new SdpEdge(interSdp1.getId(), interSdp1);

        StpType intermediateStp2 = new StpType();
        intermediateStp2.setNetworkId("urn:ogf:network:surfnet.nl:1990:inter-testbed");
        intermediateStp2.setId("urn:ogf:network:surfnet.nl:1990:inter-testbed:out");

        StpType dstStp2 = new StpType();
        dstStp2.setNetworkId("urn:ogf:network:surfnet.nl:1990:dst-testbed");
        dstStp2.setId("urn:ogf:network:surfnet.nl:1990:dst-testbed:stp-in");

        SdpType interSdp2 = createSdp(intermediateStp2, dstStp2);
        SdpEdge interEdge2 = new SdpEdge(interSdp2.getId(), interSdp2);

        StpType dstStp1 = new StpType();
        dstStp1.setNetworkId("urn:ogf:network:surfnet.nl:1990:dst-testbed");
        dstStp1.setId("urn:ogf:network:surfnet.nl:1990:dst-testbed:end");

        ServiceDomainType dstServiceDomain = new ServiceDomainType();
        dstServiceDomain.setId("urn:ogf:network:surfnet.nl:1990:dst-testbed:ServiceDomain");
        StpEdge dstEdge = new StpEdge(dstStp1.getId(), dstStp1, dstServiceDomain);

        List<GraphEdge> path = Arrays.asList(srcEdge, interEdge1, interEdge2, dstEdge);

        final Map<String, StpType> stps = new HashMap<>();
        stps.put(srcStp1.getId(), srcStp1);
        stps.put(srcStp2.getId(), srcStp2);
        stps.put(dstStp1.getId(), dstStp1);
        stps.put(dstStp2.getId(), dstStp2);
        stps.put(intermediateStp1.getId(), intermediateStp1);
        stps.put(intermediateStp2.getId(), intermediateStp2);

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

        // when
        List<StpPair> segments = subject.pullIndividualSegmentsOut(path, mockedTopology);

        // then
        assertThat(segments.size(), is(3));
        assertThat(segments.get(0).getA().getId(), is("urn:ogf:network:surfnet.nl:1990:src-testbed:start"));
        assertThat(segments.get(2).getZ().getId(), is("urn:ogf:network:surfnet.nl:1990:dst-testbed:end"));
    }

    private SdpType createSdp(StpType start, StpType end) {
        SdpType edge = new SdpType();
        edge.setId(start.getId() + "::" + end.getId());
        ResourceRefType resourceRefA = new ResourceRefType();
        resourceRefA.setId(start.getId());
        DemarcationType demarcationA = new DemarcationType();
        demarcationA.setStp(resourceRefA);
        ResourceRefType resourceRefB = new ResourceRefType();
        resourceRefB.setId(end.getId());
        DemarcationType demarcationB = new DemarcationType();
        demarcationB.setStp(resourceRefB);
        edge.setDemarcationA(demarcationA);
        edge.setDemarcationZ(demarcationB);

        return edge;
    }
}
