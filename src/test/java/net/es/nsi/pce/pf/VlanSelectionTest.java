package net.es.nsi.pce.pf;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.topology.model.NsiTopology;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Which VLAN the SEQUENTIAL chain puts on each segment. In both fixtures domains A and C swap labels
 * and B does not; vlan-loop.xml is a triangle, two-links.xml has two links between B and C.
 *
 * Runs the path-finding modules of the SEQUENTIAL chain (policy, Dijkstra, underspecified) directly.
 * ResolvePCE is left out: it only maps segments to NSAs and needs the running topology provider.
 */
@RunWith(Parameterized.class)
public class VlanSelectionTest {
    private static final String NETWORK = "urn:ogf:network:%s.example.net:2026:topology:";
    private static final ObjectFactory FACTORY = new ObjectFactory();
    private static final Map<String, NsiTopology> TOPOLOGIES = new HashMap<>();

    @Parameter(0)
    public String name;
    @Parameter(1)
    public String fixture;
    @Parameter(2)
    public String source;
    @Parameter(3)
    public String destination;
    @Parameter(4)
    public List<String> ero;
    @Parameter(5)
    public List<String> expected;

    @Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            // B cannot swap, so the VLAN on A-B is forced to the destination's.
            {"different VLANs, no ERO", "vlan-loop.xml", "domain-a:a-edge?vlan=1000", "domain-b:b-edge?vlan=1050",
                none(),
                segments("domain-a:a-edge?vlan=1000 -> domain-a:a-to-b?vlan=1050",
                         "domain-b:b-to-a?vlan=1050 -> domain-b:b-edge?vlan=1050")},
            {"same VLAN, no ERO", "vlan-loop.xml", "domain-a:a-edge?vlan=1000", "domain-b:b-edge?vlan=1000", none(),
                segments("domain-a:a-edge?vlan=1000 -> domain-a:a-to-b?vlan=1000",
                         "domain-b:b-to-a?vlan=1000 -> domain-b:b-edge?vlan=1000")},
            {"different VLANs, ERO pinned to the destination VLAN", "vlan-loop.xml", "domain-a:a-edge?vlan=1000",
                "domain-b:b-edge?vlan=1050", Arrays.asList("domain-a:a-to-b?vlan=1050"),
                segments("domain-a:a-edge?vlan=1000 -> domain-a:a-to-b?vlan=1050",
                         "domain-b:b-to-a?vlan=1050 -> domain-b:b-edge?vlan=1050")},
            // The production bug: a bare ERO into B used to pick the A-B VLAN at random and loop
            // through C. The leg into B needs 1050, so that is the VLAN chosen on A-B.
            {"different VLANs, bare ERO into the non-swapping domain", "vlan-loop.xml",
                "domain-a:a-edge?vlan=1000", "domain-b:b-edge?vlan=1050", Arrays.asList("domain-a:a-to-b"),
                segments("domain-a:a-edge?vlan=1000 -> domain-a:a-to-b?vlan=1050",
                         "domain-b:b-to-a?vlan=1050 -> domain-b:b-edge?vlan=1050")},
            {"same VLAN, bare ERO into the non-swapping domain", "vlan-loop.xml", "domain-a:a-edge?vlan=1020",
                "domain-b:b-edge?vlan=1020", Arrays.asList("domain-a:a-to-b"),
                segments("domain-a:a-edge?vlan=1020 -> domain-a:a-to-b?vlan=1020",
                         "domain-b:b-to-a?vlan=1020 -> domain-b:b-edge?vlan=1020")},
            // Between two swapping domains the SDP is widened to the whole common range and the
            // uPAs pick the VLAN; the PCE does not keep the source VLAN.
            {"same VLAN, both domains swap", "vlan-loop.xml", "domain-a:a-edge?vlan=1000",
                "domain-c:c-edge?vlan=1000", none(),
                segments("domain-a:a-edge?vlan=1000 -> domain-a:a-to-c?vlan=1000-1099",
                         "domain-c:c-to-a?vlan=1000-1099 -> domain-c:c-edge?vlan=1000")},
            // A second B-C link, but no reason to use it: no detour.
            {"two links, no detour needed", "two-links.xml", "domain-a:a-edge?vlan=1000",
                "domain-b:b-edge?vlan=1050", none(),
                segments("domain-a:a-edge?vlan=1000 -> domain-a:a-to-b?vlan=1050",
                         "domain-b:b-to-a?vlan=1050 -> domain-b:b-edge?vlan=1050")},
        });
    }

    @BeforeClass
    public static void loadTopologies() throws Exception {
        for (String fixture : Arrays.asList("vlan-loop.xml", "two-links.xml")) {
            TOPOLOGIES.put(fixture, TopologyFixture.load(fixture));
        }
    }

    @Test
    public void picksTheExpectedVlanOnEachSegment() throws Exception {
        assertThat(describe(findPath(TOPOLOGIES.get(fixture), new DijkstraPCE(), source, destination, ero)),
                is(expected));
    }

    static List<String> none() {
        return Arrays.asList();
    }

    static List<String> segments(String... segments) {
        return Arrays.asList(segments);
    }

    /** A short "domain-x:port" id in the example.net fixtures as a full STP id. */
    static String stp(String shortId) {
        String[] parts = shortId.split(":", 2);
        return String.format(NETWORK, parts[0]) + parts[1];
    }

    static String network(String domain) {
        return String.format(NETWORK, domain).replaceAll(":$", "");
    }

    static String shorten(String stpId) {
        return stpId.replaceFirst("^urn:ogf:network:([^.]+)\\.example\\.net:2026:topology:", "$1:");
    }

    static List<String> describe(List<PathSegment> path) {
        return path.stream()
                .map(segment -> shorten(segment.getA()) + " -> " + shorten(segment.getZ()))
                .collect(Collectors.toList());
    }

    /** Runs the path-finding modules of the SEQUENTIAL chain; source, destination and ERO in short form. */
    static List<PathSegment> findPath(NsiTopology topology, DijkstraPCE dijkstra, String source, String destination,
            List<String> ero) throws Exception {
        P2PServiceBaseType p2ps = FACTORY.createP2PServiceBaseType();
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);
        p2ps.setSourceSTP(stp(source));
        p2ps.setDestSTP(stp(destination));
        if (!ero.isEmpty()) {
            StpListType list = FACTORY.createStpListType();
            IntStream.range(0, ero.size()).forEach(order -> {
                OrderedStpType ordered = FACTORY.createOrderedStpType();
                ordered.setOrder(order);
                ordered.setStp(stp(ero.get(order)));
                list.getOrderedSTP().add(ordered);
            });
            p2ps.setEro(list);
        }

        ObjectAttrConstraint p2psConstraint = new ObjectAttrConstraint();
        p2psConstraint.setAttrName(Point2PointTypes.P2PS);
        p2psConstraint.setValue(p2ps);

        PCEData pceData = new PCEData();
        pceData.addConstraint(p2psConstraint);
        pceData.addConstraints(PCEConstraints.getConstraints(
                XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()),
                XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 3_600_000L),
                NsiConstants.EVTS_AGOLE, null));
        pceData.setTopology(topology);

        for (PCEModule module : Stream.of(new PolicyPCE(), dijkstra, new UnderspecifiedPCE())
                .collect(Collectors.toList())) {
            pceData = module.apply(pceData);
        }
        return pceData.getPath().getPathSegments();
    }
}
