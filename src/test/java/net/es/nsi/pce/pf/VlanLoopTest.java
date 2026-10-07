package net.es.nsi.pce.pf;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.model.NsiTopology;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Characterizes a known bug, to be fixed in DijkstraPCE: an ERO STP without a VLAN lets the leg
 * before it pick the VLAN on that SDP at random, without regard to the leg after it. On the
 * vlan-loop fixture the leg into domain B, which cannot swap labels, can then only reach the
 * destination VLAN by leaving B for C, swapping there, and entering B again. uPAs refuse a
 * segment that starts and ends on the same port, so such a path can never be reserved.
 */
public class VlanLoopTest {
    // The random pick matches the destination VLAN 1 time in 100; 5 tries make a false pass rare.
    private static final int TRIES = 5;
    private static NsiTopology topology;

    @BeforeClass
    public static void loadTopology() throws Exception {
        topology = TopologyFixture.load("vlan-loop.xml");
    }

    @Test
    public void bareEroLoopsThroughTheSwappingNeighbour() {
        boolean looped = IntStream.range(0, TRIES).anyMatch(attempt -> entersTwice(
                findPath("domain-a:a-edge?vlan=1000", "domain-b:b-edge?vlan=1050", Arrays.asList("domain-a:a-to-b")),
                VlanSelectionTest.network("domain-b")));
        assertTrue("expected a path that enters domain-b twice", looped);
    }

    private static boolean entersTwice(List<PathSegment> path, String network) {
        return path.stream().filter(segment -> network.equals(segment.getNetworkId())).count() > 1;
    }

    private static List<PathSegment> findPath(String source, String destination, List<String> ero) {
        try {
            return VlanSelectionTest.findPath(topology, source, destination, ero);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
