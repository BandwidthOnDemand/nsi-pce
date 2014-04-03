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
import net.es.nsi.pce.pf.ReachabilityPCE.Stp;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.model.NsiTopology;

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

        StringAttrConstraint source = createStringConstraint(Point2Point.SOURCESTP, sourceStp);
        StringAttrConstraint destination = createStringConstraint(Point2Point.DESTSTP, destStp);

        Map<String, Integer> costs = ImmutableMap.of("urn:ogf:network:surfnet.nl:1990:topology", 5, "urn:ogf:network:es.net:2013:topology", 10);
        ImmutableMap<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        PCEData pceData = new PCEData(source, destination);
        pceData.setReachabilityTable(reachabilityTable);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Path> path = subject.findPath(pceData);

        assertTrue(path.isPresent());

        StpPair stpPair = path.get().getStpPairs().iterator().next();

        assertThat(stpPair.getA().getId(), is(sourceStp));
        assertThat(stpPair.getA().getNetworkId(), is(peerNetworkId));

        assertThat(stpPair.getZ().getId(), is(destStp));
    }

    @Test
    public void should_find_split_path() {
        String esnetNetworkId = "urn:ogf:network:es.net:2013:topology";
        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";
        String peerNsaId = "urn:ogf:network:foo:2001:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:toplogy";

        Stp localStp = ReachabilityPCE.Stp.fromStpId(surfnetNetworkId + ":start");
        Stp remoteStp = ReachabilityPCE.Stp.fromStpId(esnetNetworkId + ":end");

        NsiTopology topology = new NsiTopology();
        SdpType sdp = createSdpType(surfnetNetworkId, peerNetworkId, "start", "intermediate");
        topology.addSdp(sdp);

        Map<String, Integer> costs = ImmutableMap.of(esnetNetworkId, 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Path> path = subject.findSplitPath(localStp, remoteStp, topology, reachabilityTable);

        assertTrue(path.isPresent());
        assertThat(path.get().getStpPairs().size(), is(2));

        StpPair pairA = path.get().getStpPairs().get(0);
        assertEquals(surfnetNetworkId, pairA.getA().getNetworkId());
        assertEquals(surfnetNetworkId, pairA.getZ().getNetworkId());

        StpPair pairB = path.get().getStpPairs().get(1);
        assertEquals(peerNetworkId, pairB.getA().getNetworkId());
        assertEquals(esnetNetworkId, pairB.getZ().getNetworkId());
    }

    private SdpType createSdpType(String networkIdA, String networkIdZ, String portA, String portZ) {
        SdpType sdp = new SdpType();
        DemarcationType demarcationA = createDemarcationType(networkIdA, portA);
        DemarcationType demarcationB = createDemarcationType(networkIdZ, portZ);
        sdp.setDemarcationA(demarcationA);
        sdp.setDemarcationZ(demarcationB);
        sdp.setId("sdpId");

        return sdp;
    }

    private DemarcationType createDemarcationType(String networkId, String portId) {
        ResourceRefType networkResource = new ResourceRefType();
        networkResource.setId(networkId);
        ResourceRefType stpResource = new ResourceRefType();
        stpResource.setId(networkId + ":" + portId);
        DemarcationType demarcation = new DemarcationType();
        demarcation.setNetwork(networkResource);
        demarcation.setStp(stpResource);
        return demarcation;
    }

    @Test
    public void should_find_forward_path() {
        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        Stp sourceStp = ReachabilityPCE.Stp.fromStpId(surfnetNetworkId + ":start");
        Stp destStp = ReachabilityPCE.Stp.fromStpId("urn:ogf:network:es.net:2013:topology:end");

        String peerNsaId = "urn:ogf:network:foo:2013:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:topology";

        Map<String, Integer> costs = ImmutableMap.of(surfnetNetworkId, 5, "urn:ogf:network:es.net:2013:topology", 10);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Path> path = subject.findForwardPath(sourceStp, destStp, reachabilityTable);

        assertTrue(path.isPresent());

        List<StpPair> stpPairs = path.get().getStpPairs();
        assertThat(stpPairs.size(), is(1));

        StpPair stpPair = stpPairs.get(0);
        assertEquals(sourceStp.getId(), stpPair.getA().getId());
        assertEquals(peerNetworkId, stpPair.getA().getNetworkId());
        assertEquals(destStp.getId(), stpPair.getZ().getId());
        assertEquals(peerNetworkId, stpPair.getA().getNetworkId());
    }

    @Test
    public void should_find_forward_path_with_equal_cost() {
        String sourceNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";
        String destNetworkId = "urn:ogf:network:es.net:2013:topology";

        Stp sourceStp = ReachabilityPCE.Stp.fromStpId(sourceNetworkId + ":start");
        Stp destStp = ReachabilityPCE.Stp.fromStpId(destNetworkId + ":end");

        String peerNsaIdOne = "urn:ogf:network:foo:2013:nsa";
        String peerNsaIdTwo = "urn:ogf:network:bar:2013:nsa";
        String peerNetworkIdOne = "urn:ogf:network:foo:2013:topology";
        String peerNetworkIdTwo = "urn:ogf:network:bar:2013:topology";

        Map<String, Integer> costsOne = ImmutableMap.of(sourceNetworkId, 5);
        Map<String, Integer> costsTwo = ImmutableMap.of(destNetworkId, 5);
        ImmutableMap<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaIdOne, costsOne, peerNsaIdTwo, costsTwo);

        when(serviceInfoProviderMock.byNsaId(peerNsaIdOne)).thenReturn(createServiceInfo(peerNsaIdOne, peerNetworkIdOne));
        when(serviceInfoProviderMock.byNsaId(peerNsaIdTwo)).thenReturn(createServiceInfo(peerNsaIdTwo, peerNetworkIdTwo));

        Optional<Path> path = subject.findForwardPath(sourceStp, destStp, reachabilityTable);

        assertThat(path.get().getStpPairs().iterator().next().getA().getNetworkId(), is(peerNetworkIdOne));
    }

    @Test
    public void should_find_cost_for_topology_id_when_having_one_peer() {
        String peerNsaId = "urn:ogf:network:nordu.net:2013:nsa";
        String peerNetworkId = "urn:ogf:network:nordu.net:2013:topology";

        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        Map<String, Integer> nordunetCosts = ImmutableMap.of("urn:ogf:network:es.net:2013:topology", 10, surfnetNetworkId, 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, nordunetCosts);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Reachability> reachability = subject.findPeerWithLowestCostToReachNetwork(surfnetNetworkId, reachabilityTable);

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

        Optional<Reachability> reachability = subject.findPeerWithLowestCostToReachNetwork(surfnetNetworkId, reachabilityTable);

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

        Optional<Reachability> reachability = subject.findPeerWithLowestCostToReachNetwork(surfnetNetworkId, ImmutableMap.of(peerNsaIdA, peerCosts, peerNsaIdZ, peerCosts));
        assertEquals(peerNetworkIdA, reachability.get().getNetworkId());

        reachability = subject.findPeerWithLowestCostToReachNetwork(surfnetNetworkId, ImmutableMap.of(peerNsaIdZ, peerCosts, peerNsaIdA, peerCosts));
        assertEquals(peerNetworkIdA, reachability.get().getNetworkId());
    }

    @Test
    public void should_not_find_a_cost_for_topology_id() {
        Map<String, Integer> nordunetCosts = ImmutableMap.of("urn:ogf:network:es.net:2013", 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of("urn:ogf:network:nordu.net:2013", nordunetCosts);

        Optional<Reachability> reachability = subject.findPeerWithLowestCostToReachNetwork("urn:ogf:network:surfnet.nl:1990", reachabilityTable);

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
            Optional<String> topologyId = ReachabilityPCE.Stp.extractNetworkId(stpId);

            assertTrue(topologyId.isPresent());
            assertEquals("urn:ogf:network:surfnet.nl:1990:topology", topologyId.get());
        }
    }

    @Test
    public void should_not_find_topology_id_in_empty_string() {
        Optional<String> topologyId = ReachabilityPCE.Stp.extractNetworkId("");

        assertFalse(topologyId.isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void should_throw_nullpointer_when_stp_id_is_null() {
        ReachabilityPCE.Stp.extractNetworkId(null);
    }

    private StringAttrConstraint createStringConstraint(String name, String value) {
        StringAttrConstraint source = new StringAttrConstraint();
        source.setAttrName(name);
        source.setValue(value);
        return source;
    }

    private ServiceInfo createServiceInfo(String nsaId, String networkId) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNsaId(nsaId);
        serviceInfo.setNetworkId(networkId);
        return serviceInfo;
    }

}
