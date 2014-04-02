package net.es.nsi.pce.pf;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.pf.ReachabilityPCE.Reachability;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ReachabilityPCETest {

    @InjectMocks private ReachabilityPCE subject;

    @Mock private ServiceInfoProvider serviceInfoProviderMock;

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_missing_source_stp() {
        PCEData pceData = new PCEData();
        subject.findPath(pceData);
    }

    @Test
    @Ignore("todo")
    public void should_calculate_path_if_both_src_and_dest_is_in_topology() {
    }

    @Test
    @Ignore("todo")
    public void should_splitup_request_if_src_is_in_toplogy() {
    }

    @Test
    public void should_forward_request_to_nearest_nsa() {
        String sourceStp = "urn:ogf:network:surfnet.nl:1990:testbed:start";
        String destStp = "urn:ogf:network:es.net:2013:topology:end";

        String peerNsaId = "urn:ogf:network:foo:2013:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:toplogy";

        StringAttrConstraint source = new StringAttrConstraint();
        source.setValue(sourceStp);
        source.setAttrName(Point2Point.SOURCESTP);
        StringAttrConstraint destination = new StringAttrConstraint();
        destination.setValue(destStp);
        destination.setAttrName(Point2Point.DESTSTP);

        PCEData pceData = new PCEData();
        pceData.addConstraint(source);
        pceData.addConstraint(destination);
        Map<String, Integer> costs = ImmutableMap.of("urn:ogf:network:surfnet.nl:1990:topology", 5, "urn:ogf:network:es.net:2013:topology", 10);
        pceData.setReachabilityTable(ImmutableMap.of(peerNsaId, costs));

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        PCEData result = subject.apply(pceData);

        assertNotNull(result.getPath());

        StpPair stpPair = result.getPath().getStpPairs().iterator().next();

        assertThat(stpPair.getA().getId(), is(sourceStp));
        assertThat(stpPair.getA().getNetworkId(), is(peerNetworkId));

        assertThat(stpPair.getZ().getId(), is(destStp));
    }

    @Test
    public void should_find_forward_path() {
        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        String sourceStp = surfnetNetworkId + ":start";
        String destStp = "urn:ogf:network:es.net:2013:topology:end";

        String peerNsaId = "urn:ogf:network:foo:2013:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:topology";

        PCEData pceData = new PCEData();
        Map<String, Integer> costs = ImmutableMap.of(surfnetNetworkId, 5, "urn:ogf:network:es.net:2013:topology", 10);
        pceData.setReachabilityTable(ImmutableMap.of(peerNsaId, costs));

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Path> path = subject.findForwardPath(sourceStp, destStp, pceData);

        assertTrue(path.isPresent());

        List<StpPair> stpPairs = path.get().getStpPairs();
        assertThat(stpPairs.size(), is(1));

        StpPair stpPair = stpPairs.get(0);
        assertEquals(sourceStp, stpPair.getA().getId());
        assertEquals(peerNetworkId, stpPair.getA().getNetworkId());
        assertEquals(destStp, stpPair.getZ().getId());
        assertEquals(peerNetworkId, stpPair.getA().getNetworkId());
    }

    @Test
    public void should_find_cost_for_topology_id_when_having_one_peer() {
        String peerNsaId = "urn:ogf:network:nordu.net:2013:nsa";
        String peerNetworkId = "urn:ogf:network:nordu.net:2013:topology";

        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        Map<String, Integer> nordunetCosts = ImmutableMap.of("urn:ogf:network:es.net:2013:topology", 10, surfnetNetworkId, 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, nordunetCosts);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Reachability> reachability = subject.determineCost(surfnetNetworkId, reachabilityTable);

        assertTrue(reachability.isPresent());
        assertEquals(new Integer(5), reachability.get().getCost());
        assertEquals(peerNetworkId, reachability.get().getNetworkId());
    }

    @Test
    public void should_find_cost_for_topology_id_when_having_multiple_peers() {
        String peerNsaIdExpensive = "urn:ogf:network:foo:2013:nsa";
        String peerNsaIdCheap = "urn:ogf:network:bar:2000:nsa";

        String peerNetworkIdExpensive = "urn:ogf:network:foo:2013:topology";
        String peerNetworkIdCheap = "urn:ogf:network:bar:2000:topology";

        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        Map<String, Integer> peerExpensiveCosts = ImmutableMap.of(surfnetNetworkId, 5);
        Map<String, Integer> peerCheapCosts = ImmutableMap.of(surfnetNetworkId, 2);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaIdExpensive, peerExpensiveCosts, peerNsaIdCheap, peerCheapCosts);

        when(serviceInfoProviderMock.byNsaId(peerNsaIdExpensive)).thenReturn(createServiceInfo(peerNsaIdExpensive, peerNetworkIdExpensive));
        when(serviceInfoProviderMock.byNsaId(peerNsaIdCheap)).thenReturn(createServiceInfo(peerNsaIdCheap, peerNetworkIdCheap));

        Optional<Reachability> reachability = subject.determineCost(surfnetNetworkId, reachabilityTable);

        assertTrue(reachability.isPresent());
        assertEquals(new Integer(2), reachability.get().getCost());
        assertEquals(peerNetworkIdCheap, reachability.get().getNetworkId());
    }

    @Test
    public void should_find_cost_for_topology_id_when_having_multiple_peers_with_equal_cost() {
        String peerNsaIdZ = "urn:ogf:network:zzz:2013:nsa";
        String peerNsaIdA = "urn:ogf:network:aaa:2000:nsa";

        String peerNetworkIdZ = "urn:ogf:network:zzz:2013:topology";
        String peerNetworkIdA = "urn:ogf:network:aaa:2000:topology";

        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        Map<String, Integer> peerCosts = ImmutableMap.of(surfnetNetworkId, 2);
        when(serviceInfoProviderMock.byNsaId(peerNsaIdZ)).thenReturn(createServiceInfo(peerNsaIdZ, peerNetworkIdZ));
        when(serviceInfoProviderMock.byNsaId(peerNsaIdA)).thenReturn(createServiceInfo(peerNsaIdA, peerNetworkIdA));

        Optional<Reachability> reachability = subject.determineCost(surfnetNetworkId, ImmutableMap.of(peerNsaIdA, peerCosts, peerNsaIdZ, peerCosts));
        assertEquals(peerNetworkIdA, reachability.get().getNetworkId());

        reachability = subject.determineCost(surfnetNetworkId, ImmutableMap.of(peerNsaIdZ, peerCosts, peerNsaIdA, peerCosts));
        assertEquals(peerNetworkIdA, reachability.get().getNetworkId());
    }

    @Test
    public void should_not_find_a_cost_for_topology_id() {
        Map<String, Integer> nordunetCosts = ImmutableMap.of("urn:ogf:network:es.net:2013", 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of("urn:ogf:network:nordu.net:2013", nordunetCosts);

        Optional<Reachability> reachability = subject.determineCost("urn:ogf:network:surfnet.nl:1990", reachabilityTable);

        assertFalse(reachability.isPresent());
    }

    @Test
    public void should_find_topology_id_from_stp_id() {
        List<String> stpIds = Arrays.asList(
            "urn:ogf:network:surfnet.nl:1990:topology:foobar",
            "urn:ogf:network:surfnet.nl:1990:topology:1",
            "urn:ogf:network:surfnet.nl:1990:topology:foo:bar",
            "urn:ogf:network:surfnet.nl:1990:topology");

        for (String stpId : stpIds) {
            Optional<String> topologyId = subject.extractNetworkId(stpId);

            assertTrue(topologyId.isPresent());
            assertEquals("urn:ogf:network:surfnet.nl:1990:topology", topologyId.get());
        }
    }

    @Test
    public void should_not_find_topology_id_in_empty_string() {
        Optional<String> topologyId = subject.extractNetworkId("");

        assertFalse(topologyId.isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void should_throw_nullpointer_when_stp_id_is_null() {
        subject.extractNetworkId(null);
    }

    private ServiceInfo createServiceInfo(String nsaId, String networkId) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNsaId(nsaId);
        serviceInfo.setNetworkId(networkId);
        return serviceInfo;
    }

}
