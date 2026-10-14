package net.es.nsi.pce.pf;

import java.util.Arrays;
import java.util.List;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.simple.SimpleStp;
import net.es.nsi.pce.topology.model.NsiTopology;
import static net.es.nsi.pce.pf.VlanSelectionTest.describe;
import static net.es.nsi.pce.pf.VlanSelectionTest.findPath;
import static net.es.nsi.pce.pf.VlanSelectionTest.network;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Re-entering a network within one leg of a request: refused out and back through one port, allowed
 * through another port only as a last resort, and always allowed across ERO points. The cases with a
 * single expected path are rows in VlanSelectionTest.
 */
public class VlanLoopTest {
    // The old router picks the VLAN at random; it matches the destination 1 time in 100.
    private static final int RUNS = 5;
    private static NsiTopology vlanLoop;
    private static NsiTopology twoLinks;

    @BeforeClass
    public static void loadTopologies() throws Exception {
        vlanLoop = TopologyFixture.load("vlan-loop.xml");
        twoLinks = TopologyFixture.load("two-links.xml");
    }

    @Test
    public void bareEroLoopsWhenLoopAvoidanceIsOff() throws Exception {
        DijkstraPCE perLeg = new DijkstraPCE();
        perLeg.setAvoidLoops(false);
        boolean looped = false;
        for (int run = 0; run < RUNS && !looped; run++) {
            looped = entersTwice(findPath(vlanLoop, perLeg, "domain-a:a-edge?vlan=1000", "domain-b:b-edge?vlan=1050",
                    Arrays.asList("domain-a:a-to-b")), "domain-b");
        }
        assertTrue("expected a path that enters domain-b twice", looped);
    }

    /**
     * B cannot swap and the A-B VLAN is pinned; only a detour through C changes it. The detour must
     * leave B on one B-C link and come back on the other; which link goes first does not matter.
     */
    @Test
    public void detourThroughDifferentPortsIsALastResort() throws Exception {
        List<String> path = describe(findPath(twoLinks, new DijkstraPCE(), "domain-a:a-edge?vlan=1000",
                "domain-b:b-edge?vlan=1050", Arrays.asList("domain-a:a-to-b?vlan=1010")));
        assertThat(path.size(), is(4));
        assertThat(path.get(0), is("domain-a:a-edge?vlan=1000 -> domain-a:a-to-b?vlan=1010"));
        assertThat(path.get(3).endsWith("?vlan=1050 -> domain-b:b-edge?vlan=1050"), is(true));
        String out = SimpleStp.getId(path.get(1).split(" -> ")[1]);
        String back = SimpleStp.getId(path.get(3).split(" -> ")[0]);
        assertTrue(out + " and " + back + " must be the two B-C links",
                out.startsWith("domain-b:b-to-c") && back.startsWith("domain-b:b-to-c") && !out.equals(back));
    }

    /** The ERO leaves B for C and comes back through the other link: a hairpin asked for. */
    @Test
    public void hairpinAcrossEroPointsIsKept() throws Exception {
        List<PathSegment> path = findPath(twoLinks, new DijkstraPCE(), "domain-a:a-edge?vlan=1000",
                "domain-d:d-edge?vlan=1000", Arrays.asList("domain-b:b-to-c", "domain-c:c-to-b2"));
        assertTrue("expected the requested hairpin through domain-c", entersTwice(path, "domain-b"));
        assertThat(describe(path).get(1).startsWith("domain-b:b-to-a"), is(true));
        assertThat(describe(path).get(2).startsWith("domain-c:c-to-b?"), is(true));
    }

    private static boolean entersTwice(List<PathSegment> path, String domain) {
        return path.stream().filter(segment -> network(domain).equals(segment.getNetworkId())).count() > 1;
    }
}
