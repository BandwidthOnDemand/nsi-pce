package net.es.nsi.pce.pf.api.gof3;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

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

    final String ourNetworkId = "urn:org:network:our:1990:network:topo";

    @Before
    public void setUp() throws Exception {
        nsiTopology = new NsiTopology();
        nsiTopology.setLocalNetworks(Arrays.asList(ourNetworkId));

        when(topologyProvider.getTopology()).thenReturn(nsiTopology);
    }

    @Test
    public void no_peers_found_return_only_local() {
        final List<VectorType> result = subject.getCurrentReachabilityInfo();
        assertTrue(result.size() == 1);
        final Optional<VectorType> ours = Iterables.tryFind(result, new Predicate<VectorType>() {
            @Override
            public boolean apply(VectorType input) {
                return input.getId().equals(ourNetworkId);
            }
        });
        assertTrue(ours.isPresent());
        assertTrue(ours.get().getCost() == 0);
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
        final List<VectorType> result = subject.getCurrentReachabilityInfo();
        assertThat("table must contain our own network, and what is advertised by our peer", result.size(), is(2));

        final Optional<VectorType> peerVector = Iterables.tryFind(result, new Predicate<VectorType>() {
            @Override
            public boolean apply(VectorType input) {
                return input.getId().equals(peerNetworkId);
            }
        });
        assertTrue(peerVector.isPresent());
        assertTrue(peerVector.get().getCost() == originalPeerVector.getCost() + 1);
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
        originalPeerVector.setId(ourNetworkId);
        originalPeerVector.setCost(23);
        reachabilityType.getVector().add(originalPeerVector);

        peerNsa.getReachability().add(reachabilityType);
        final List<VectorType> result = subject.getCurrentReachabilityInfo();
        assertThat("table must contain our own network only", result.size(), is(1));

        // must contain vector for our self with cost = 0
        final Optional<VectorType> ours = Iterables.tryFind(result, new Predicate<VectorType>() {
            @Override
            public boolean apply(VectorType input) {
                return input.getId().equals(ourNetworkId);
            }
        });
        assertTrue(ours.isPresent());
        assertTrue(ours.get().getCost() == 0);
    }
}
