package net.es.nsi.pce.pf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.pf.graph.EdgeTrasnsformer;
import net.es.nsi.pce.pf.graph.GraphEdge;
import net.es.nsi.pce.pf.route.Route;
import net.es.nsi.pce.pf.route.StpTypeBundle;
import net.es.nsi.pce.topology.model.NsiStpFactory;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes all legs of a request together, so the VLAN chosen where one leg ends fits the leg after it.
 *
 * Each ERO STP splits a request into legs. The per-leg router in DijkstraPCE picks the VLAN at an ERO
 * point at random; a domain that cannot swap labels may then only reach the next VLAN by leaving and
 * re-entering it. Here a leg never goes out and back through one port, which gives a segment the uPA
 * refuses, and complete paths are ranked by:
 *
 *   1. fewest re-entries into a network through another port, a last resort for changing a VLAN;
 *   2. fewest hops;
 *   3. fewest VLAN changes between consecutive STPs, so the source VLAN is kept where it can be.
 *
 * Re-entry is only counted within a leg. A path that leaves a network at one ERO STP and comes back
 * at a later one is a hairpin the requester asked for.
 */
final class LoopAvoidingRouter {
    private static final Logger LOG = LoggerFactory.getLogger(LoopAvoidingRouter.class);

    private final NsiTopology topology;
    private final DirectionalityType directionality;
    private final EdgeTrasnsformer weights;
    private final List<Route> routes;

    LoopAvoidingRouter(Request request) {
        this.topology = request.getNsiTopology();
        this.directionality = request.getDirectionality();
        this.weights = new EdgeTrasnsformer(request.getPceData());
        this.routes = request.getRo().getRoutes();
    }

    List<GraphEdge> route() {
        List<Partial> partials = new ArrayList<>();
        partials.add(Partial.EMPTY);
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            partials = extend(partials, route, i == routes.size() - 1);
            if (partials.isEmpty()) {
                throw Exceptions.noPathFound("No path found using provided criteria sourceStp="
                        + route.getBundleA().getSimpleStp().getId() + ", and destStp="
                        + route.getBundleZ().getSimpleStp().getId());
            }
        }
        Partial best = partials.stream().min(Partial.RANKING).get();
        LOG.info("route: {} hops, {} re-entries, {} VLAN changes",
                best.edges.size(), best.reentries, best.vlanChanges);
        return best.edges;
    }

    /**
     * Extends each partial path with each viable route for the next leg. Only the best partial path
     * per leg end is kept: the next leg depends on where this one ends, not on how it got there.
     */
    private List<Partial> extend(List<Partial> partials, Route route, boolean lastLeg) {
        // One graph for the whole leg, holding the start STPs of every partial path.
        Map<Partial, StpTypeBundle> sources = new LinkedHashMap<>();
        StpTypeBundle allSources = new StpTypeBundle();
        for (Partial partial : partials) {
            StpTypeBundle source = partial.end
                    .map(end -> StpTypeBundle.getPeerBundle(topology, end, directionality).orNull())
                    .orElse(route.getBundleA());
            if (!source.isEmpty()) {
                sources.put(partial, source);
                source.values().forEach(allSources::addStpType);
            }
        }
        if (sources.isEmpty()) {
            return new ArrayList<>();
        }
        LegGraph graph = new LegGraph(allSources, route.getBundleZ(), weights, topology);

        Map<String, Partial> bestPerEnd = new HashMap<>();
        sources.forEach((partial, source) -> extendOne(graph, partial, source, route.getBundleZ(), lastLeg)
                .forEach(next -> bestPerEnd.merge(next.end.get().getId(), next, Partial::better)));
        return new ArrayList<>(bestPerEnd.values());
    }

    /**
     * The best extension of one partial path to each end. Start and end pairs are tried shortest
     * unconstrained path first. Once a pair is routed without re-entry, a pair whose unconstrained
     * path is already longer cannot rank better and is skipped: for that end on earlier legs, where a
     * longer route to another end may still suit the next leg, and for every end on the last leg.
     * With a VLAN range on both sides of a non-swapping domain, this skips the costly conflict search
     * for every mismatched VLAN pair.
     */
    private List<Partial> extendOne(LegGraph graph, Partial partial, StpTypeBundle source, StpTypeBundle ends,
            boolean lastLeg) {
        List<Pair> pairs = new ArrayList<>();
        for (StpType a : source.values()) {
            for (StpType z : ends.values()) {
                int length = graph.shortestLength(a, z);
                if (length >= 0) {
                    pairs.add(new Pair(a, z, length));
                }
            }
        }
        pairs.sort(Comparator.comparingInt(pair -> pair.length));

        Map<String, Partial> bestPerEnd = new HashMap<>();
        Map<String, Integer> cleanPerEnd = new HashMap<>();
        int cleanAnyEnd = Integer.MAX_VALUE;
        for (Pair pair : pairs) {
            int bound = lastLeg ? cleanAnyEnd : cleanPerEnd.getOrDefault(pair.z.getId(), Integer.MAX_VALUE);
            if (pair.length > bound) {
                continue;
            }
            Optional<List<GraphEdge>> leg = graph.leg(pair.a, pair.z);
            if (leg.isPresent()) {
                Partial next = partial.extend(graph, leg.get());
                bestPerEnd.merge(pair.z.getId(), next, Partial::better);
                if (next.reentries == partial.reentries) {
                    cleanPerEnd.merge(pair.z.getId(), leg.get().size(), Math::min);
                    cleanAnyEnd = Math.min(cleanAnyEnd, leg.get().size());
                }
            }
        }
        return new ArrayList<>(bestPerEnd.values());
    }

    private static final class Pair {
        final StpType a;
        final StpType z;
        final int length;

        Pair(StpType a, StpType z, int length) {
            this.a = a;
            this.z = z;
            this.length = length;
        }
    }

    /** A path through the legs routed so far, with the counts the ranking compares. */
    private static final class Partial {
        static final Partial EMPTY = new Partial(new ArrayList<>(), Optional.empty(), 0, 0);
        static final Comparator<Partial> RANKING = Comparator.<Partial>comparingInt(p -> p.reentries)
                .thenComparingInt(p -> p.edges.size())
                .thenComparingInt(p -> p.vlanChanges);

        final List<GraphEdge> edges;
        final Optional<StpType> end;
        final int reentries;
        final int vlanChanges;

        Partial(List<GraphEdge> edges, Optional<StpType> end, int reentries, int vlanChanges) {
            this.edges = edges;
            this.end = end;
            this.reentries = reentries;
            this.vlanChanges = vlanChanges;
        }

        Partial extend(LegGraph graph, List<GraphEdge> leg) {
            List<StpType> stps = graph.stps(leg);
            List<StpType> sequence = new ArrayList<>();
            end.ifPresent(sequence::add);
            sequence.addAll(stps);
            int changes = 0;
            for (int i = 1; i < sequence.size(); i++) {
                changes += NsiStpFactory.labelEquals(sequence.get(i).getLabel(), sequence.get(i - 1).getLabel()) ? 0 : 1;
            }

            List<GraphEdge> joined = new ArrayList<>(edges);
            joined.addAll(leg);
            return new Partial(joined, Optional.of(stps.get(stps.size() - 1)),
                    reentries + LegGraph.reentries(stps).size(), vlanChanges + changes);
        }

        static Partial better(Partial x, Partial y) {
            return RANKING.compare(x, y) <= 0 ? x : y;
        }
    }
}
