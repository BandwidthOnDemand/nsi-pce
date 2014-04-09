package net.es.nsi.pce.pf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

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


@RunWith(MockitoJUnitRunner.class)
public class ReachabilityPCETest {

    private ReachabilityPCE subject;

    private ServiceInfoProvider serviceInfoProviderMock;

    private final static String LOCAL_NETWORK_ID = "urn:ogf:network:local.net:1970:topology";

    @Before
    public void setUp() throws Exception {
        serviceInfoProviderMock = mock(ServiceInfoProvider.class);
        subject = new ReachabilityPCE(serviceInfoProviderMock, LOCAL_NETWORK_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_missing_source_stp() {
        PCEData pceData = new PCEData();
        subject.findPath(pceData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_missing_connection_trace() {
        String sourceStp = LOCAL_NETWORK_ID + ":start";
        String destStp = LOCAL_NETWORK_ID + ":end";

        StringAttrConstraint source = createStringConstraint(Point2Point.SOURCESTP, sourceStp);
        StringAttrConstraint destination = createStringConstraint(Point2Point.DESTSTP, destStp);

        PCEData pceData = new PCEData(source, destination);
        subject.apply(pceData);
    }

    @Test
    public void should_calculate_path_if_both_src_and_dest_belongs_to_local_network() {
        String sourceStp = LOCAL_NETWORK_ID + ":start";
        String destStp = LOCAL_NETWORK_ID + ":end";

        StringAttrConstraint source = createStringConstraint(Point2Point.SOURCESTP, sourceStp);
        StringAttrConstraint destination = createStringConstraint(Point2Point.DESTSTP, destStp);

        PCEData pceData = new PCEData(source, destination);
        pceData.setConnectionTrace(Collections.EMPTY_LIST);

        PCEData reply = subject.apply(pceData);
        verifyZeroInteractions(serviceInfoProviderMock);
        Path path = reply.getPath();

        assertNotNull(path);
        assertTrue(path.getStpPairs().size() == 1);
        StpPair pair = path.getStpPairs().get(0);
        assertTrue(pair.getA().getNetworkId().equals(pair.getZ().getNetworkId())
                && pair.getA().getNetworkId().equals(LOCAL_NETWORK_ID));
    }

    @Test
    public void should_split_up_request_if_src_or_dest_belongs_to_local_network() {
        String sourceStp = LOCAL_NETWORK_ID + ":start";
        String destNetworkId= "urn:ogf:network:bar:1980:topology";
        String destStp = destNetworkId + ":end";

        String peerNsaId = "urn:ogf:network:foo:2013:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:topology";

        StringAttrConstraint source = createStringConstraint(Point2Point.SOURCESTP, sourceStp);
        StringAttrConstraint destination = createStringConstraint(Point2Point.DESTSTP, destStp);

        Map<String, Integer> costs = ImmutableMap.of(destNetworkId, 5, "urn:ogf:network:es.net:2013:topology", 10);
        ImmutableMap<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        NsiTopology topology = new NsiTopology();
        SdpType sdp = createSdpType(LOCAL_NETWORK_ID, peerNetworkId, "surf-intermediate", "peer-intermediate");
        topology.addSdp(sdp);

        PCEData pceData = new PCEData(source, destination);
        pceData.setConnectionTrace(Collections.EMPTY_LIST);

        pceData.setReachabilityTable(reachabilityTable);
        pceData.setTopology(topology);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        PCEData reply = subject.apply(pceData);
        Path path = reply.getPath();

        assertNotNull(path);
        assertEquals(2, path.getStpPairs().size());
        // the first pair must be the segment we can provide: from source stp to the SDP that talks to the appropriate peer
        assertPair(path.getStpPairs().get(0), LOCAL_NETWORK_ID, LOCAL_NETWORK_ID, sourceStp, LOCAL_NETWORK_ID + ":surf-intermediate");

        // the second pair should be the remainder of the path to be computed by upstream peer
        assertPair(path.getStpPairs().get(1), peerNetworkId, destNetworkId, peerNetworkId + ":peer-intermediate", destStp);
    }

    @Test
    public void should_forward_request_to_nearest_nsa() {
        String sourceStp = "urn:ogf:network:surfnet.nl:1990:testbed:start";
        String destStp = "urn:ogf:network:es.net:2013:topology:end";

        String peerNsaId = "urn:ogf:network:foo:2013:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:topology";

        StringAttrConstraint source = createStringConstraint(Point2Point.SOURCESTP, sourceStp);
        StringAttrConstraint destination = createStringConstraint(Point2Point.DESTSTP, destStp);

        Map<String, Integer> costs = ImmutableMap.of("urn:ogf:network:surfnet.nl:1990:topology", 5, "urn:ogf:network:es.net:2013:topology", 10);
        ImmutableMap<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        PCEData pceData = new PCEData(source, destination);
        pceData.setReachabilityTable(reachabilityTable);
        pceData.setConnectionTrace(Collections.<String>emptyList());

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Path> path = subject.findPath(pceData);

        assertTrue(path.isPresent());
        assertPair(path.get().getStpPairs().iterator().next(), peerNetworkId, peerNetworkId, sourceStp, destStp);
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
        SdpType sdp = createSdpType(surfnetNetworkId, peerNetworkId, "surf-intermediate", "peer-intermediate");
        topology.addSdp(sdp);

        Map<String, Integer> costs = ImmutableMap.of(esnetNetworkId, 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        Optional<Path> path = subject.findSplitPath(localStp, remoteStp, topology, reachabilityTable, Collections.<String>emptyList());

        assertTrue(path.isPresent());
        assertThat(path.get().getStpPairs().size(), is(2));

        assertPair(path.get().getStpPairs().get(0), surfnetNetworkId, surfnetNetworkId, localStp.getId(), surfnetNetworkId + ":surf-intermediate");
        assertPair(path.get().getStpPairs().get(1), peerNetworkId, esnetNetworkId, peerNetworkId + ":peer-intermediate", remoteStp.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_dectect_loop_when_splitting_path() {
        String esnetNetworkId = "urn:ogf:network:es.net:2013:topology";
        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";
        String peerNsaId = "urn:ogf:network:foo:2001:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:toplogy";

        Stp localStp = ReachabilityPCE.Stp.fromStpId(surfnetNetworkId + ":start");
        Stp remoteStp = ReachabilityPCE.Stp.fromStpId(esnetNetworkId + ":end");

        Map<String, Integer> costs = ImmutableMap.of(esnetNetworkId, 5);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        subject.findSplitPath(localStp, remoteStp, new NsiTopology(), reachabilityTable, Arrays.asList(peerNsaId));
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

        Optional<Path> path = subject.findForwardPath(sourceStp, destStp, reachabilityTable, Collections.<String>emptyList());

        assertTrue(path.isPresent());

        List<StpPair> stpPairs = path.get().getStpPairs();
        assertThat(stpPairs.size(), is(1));

        assertPair(stpPairs.get(0), peerNetworkId, peerNetworkId, sourceStp.getId(), destStp.getId());
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

        Optional<Path> path = subject.findForwardPath(sourceStp, destStp, reachabilityTable, Collections.<String>emptyList());

        assertThat(path.get().getStpPairs().iterator().next().getA().getNetworkId(), is(peerNetworkIdOne));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_detect_loop_when_finding_forward_path() {
        String surfnetNetworkId = "urn:ogf:network:surfnet.nl:1990:topology";

        Stp sourceStp = ReachabilityPCE.Stp.fromStpId(surfnetNetworkId + ":start");
        Stp destStp = ReachabilityPCE.Stp.fromStpId("urn:ogf:network:es.net:2013:topology:end");

        String peerNsaId = "urn:ogf:network:foo:2013:nsa";
        String peerNetworkId = "urn:ogf:network:foo:2013:topology";

        Map<String, Integer> costs = ImmutableMap.of(surfnetNetworkId, 5, "urn:ogf:network:es.net:2013:topology", 10);
        Map<String, Map<String, Integer>> reachabilityTable = ImmutableMap.of(peerNsaId, costs);

        when(serviceInfoProviderMock.byNsaId(peerNsaId)).thenReturn(createServiceInfo(peerNsaId, peerNetworkId));

        subject.findForwardPath(sourceStp, destStp, reachabilityTable, Arrays.asList(peerNsaId));
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

    private void assertPair(StpPair pair, String networkA, String networkZ, String portA, String portZ) {
        assertEquals(networkA, pair.getA().getNetworkId());
        assertEquals(networkZ, pair.getZ().getNetworkId());
        assertEquals(portA, pair.getA().getId());
        assertEquals(portZ, pair.getZ().getId());
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

}
