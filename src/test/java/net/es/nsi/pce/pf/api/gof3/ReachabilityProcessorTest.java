package net.es.nsi.pce.pf.api.gof3;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ReachabilityType;
import net.es.nsi.pce.topology.jaxb.VectorType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.TopologyProvider;

@RunWith(MockitoJUnitRunner.class)
public class ReachabilityProcessorTest {

    @InjectMocks
    private ReachabilityProcessor subject;

    @Mock
    private TopologyProvider topologyProvider;

    private NsiTopology nsiTopology;

    private static final String OUR_NETWORK_ID = "urn:org:network:our:1990:network:topo";

    @Before
    public void setUp() throws Exception {
        nsiTopology = new NsiTopology();
        nsiTopology.setLocalNetworks(Arrays.asList(OUR_NETWORK_ID));

        when(topologyProvider.getTopology()).thenReturn(nsiTopology);
    }

    @Test
    public void no_peers_found_return_only_local_with_cost_1() {
        final Map<String,Integer> result = subject.getCurrentReachabilityInfo();
        assertTrue(result.size() == 1);

        assertTrue(result.containsKey(OUR_NETWORK_ID));
        assertTrue(result.get(OUR_NETWORK_ID) == 1);
    }

    @Test
    public void one_peer_which_in_turn_has_one_peer() {
        final String peerNsaId = "peer nsa id";
        NsaType peerNsa = new NsaType();
        peerNsa.setId(peerNsaId);
        nsiTopology.addNsa(peerNsa);

        final ReachabilityType reachabilityType = new ReachabilityType();
        reachabilityType.setId(peerNsaId);

        final VectorType originalPeerVector = new VectorType();
        final String peerNetworkId = "peer network id";
        originalPeerVector.setId(peerNetworkId);
        originalPeerVector.setCost(1);
        reachabilityType.getVector().add(originalPeerVector);

        peerNsa.getReachability().add(reachabilityType);
        final Map<String,Integer> result = subject.getCurrentReachabilityInfo();

        assertThat("table must contain our own network, and what is advertised by our peer", result.size(), is(2));
        assertTrue(result.containsKey(peerNetworkId));
        assertTrue(result.get(peerNetworkId) == originalPeerVector.getCost() + 1);
    }

    @Test
    public void dont_re_advertise_own_networks() {
        final String peerNsaId = "peer nsa id";
        NsaType peerNsa = new NsaType();
        peerNsa.setId(peerNsaId);
        nsiTopology.addNsa(peerNsa);

        final ReachabilityType reachabilityType = new ReachabilityType();
        reachabilityType.setId(peerNsaId);

        final VectorType originalPeerVector = new VectorType();
        originalPeerVector.setId(OUR_NETWORK_ID);
        originalPeerVector.setCost(23);
        reachabilityType.getVector().add(originalPeerVector);

        peerNsa.getReachability().add(reachabilityType);
        final Map<String,Integer> result = subject.getCurrentReachabilityInfo();
        assertThat("table must contain our own network only", result.size(), is(1));

        // must contain vector for our self with cost = 1
        assertTrue(result.containsKey(OUR_NETWORK_ID));
        assertTrue(result.get(OUR_NETWORK_ID) == 1);
    }

    @Test
    public void when_two_peers_advertise_the_same_network_we_must_only_readvertise_the_one_with_lowest_cost() {

        final String twiceAdvertisedNetworkId = "the network id that is advertised by both peers";
        final Integer cheapPeerCost = 5;
        final Integer expensivePeerCost = 10;

        // setup the cheap peer
        final String cheapPeerNsaId = "cheap peer nsa id";
        NsaType cheapPeerNsa = new NsaType();
        cheapPeerNsa.setId(cheapPeerNsaId);
        final ReachabilityType cheapPeerReachabilityType = new ReachabilityType();
        cheapPeerReachabilityType.setId(cheapPeerNsaId);
        final VectorType originalCheapPeerVector = new VectorType();
        cheapPeerReachabilityType.getVector().add(originalCheapPeerVector);
        originalCheapPeerVector.setId(twiceAdvertisedNetworkId);
        originalCheapPeerVector.setCost(cheapPeerCost);
        cheapPeerNsa.getReachability().add(cheapPeerReachabilityType);

        // ... and the expensive one
        final String expensivePeerNsaId = "expensive peer nsa id";
        NsaType expensivePeerNsa = new NsaType();
        expensivePeerNsa.setId(expensivePeerNsaId);
        final ReachabilityType expensivePeerReachabilityType = new ReachabilityType();
        expensivePeerReachabilityType.setId(expensivePeerNsaId);
        final VectorType originalExpensivePeerVector = new VectorType();
        expensivePeerReachabilityType.getVector().add(originalExpensivePeerVector);
        originalExpensivePeerVector.setId(twiceAdvertisedNetworkId);
        originalExpensivePeerVector.setCost(expensivePeerCost);
        expensivePeerNsa.getReachability().add(expensivePeerReachabilityType);

        final Map<String,Integer> result = subject.makeResult(Arrays.asList(cheapPeerNsa, expensivePeerNsa), Collections.<String>emptyList(), new HashMap<String, Integer>());
        assertThat("table must contain only the peer", result.size(), is(1));

        assertTrue(result.containsKey(twiceAdvertisedNetworkId));
        assertThat("only the cheapest must be in there", result.get(twiceAdvertisedNetworkId), equalTo(originalCheapPeerVector.getCost() + 1));
    }
}
