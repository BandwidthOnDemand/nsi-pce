package net.es.nsi.pce.pf;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.graph.GraphEdge;
import net.es.nsi.pce.pf.graph.GraphUtils;
import net.es.nsi.pce.pf.graph.GraphVertex;
import net.es.nsi.pce.pf.graph.SdpEdge;
import net.es.nsi.pce.pf.route.StpTypeBundle;
import net.es.nsi.pce.pf.simple.SimpleStp;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.apache.commons.collections15.Transformer;

/**
 * The routing graph of one leg, from GraphUtils.buildRoutingGraph, with the constrained searches
 * LoopAvoidingRouter needs on top of plain shortest paths.
 */
final class LegGraph {
    // Nested conflicts shortestWithout resolves exactly; each level doubles the Dijkstra runs.
    private static final int MAX_BRANCHINGS = 4;
    // Heavier than any real path, so Dijkstra only crosses a blocked edge when nothing else connects.
    private static final Number BLOCKED = 1e9;

    private final NsiTopology topology;
    private final Map<String, GraphVertex> vertices = new HashMap<>();
    private final Graph<GraphVertex, GraphEdge> graph;
    private final Transformer<GraphEdge, Number> weights;
    private final DijkstraShortestPath<GraphVertex, GraphEdge> unblocked;
    // JUNG caches each search per source vertex, so a search per blocked set is reused across pairs.
    private final Map<Set<GraphEdge>, DijkstraShortestPath<GraphVertex, GraphEdge>> blockedSearches = new HashMap<>();

    LegGraph(StpTypeBundle source, StpTypeBundle destination, Transformer<GraphEdge, Number> weights,
            NsiTopology topology) {
        this.topology = topology;
        this.weights = weights;
        this.graph = GraphUtils.buildRoutingGraph(topology, source, destination, vertices);
        this.unblocked = new DijkstraShortestPath<>(graph, weights, true);
    }

    /** Edges on the shortest unconstrained path from a to z, or -1 if there is none. */
    int shortestLength(StpType a, StpType z) {
        if (!vertices.containsKey(a.getId()) || !vertices.containsKey(z.getId())) {
            return -1;
        }
        List<GraphEdge> path = shortest(a, z, Collections.<GraphEdge>emptySet());
        return path.isEmpty() ? -1 : path.size();
    }

    /**
     * The shortest path from a to z that enters each network once, else the shortest that does not
     * go out and back through one port, else empty.
     */
    Optional<List<GraphEdge>> leg(StpType a, StpType z) {
        Optional<List<GraphEdge>> path = shortestWithout(a, z, this::reentry, Collections.<GraphEdge>emptySet(),
                MAX_BRANCHINGS);
        return path.isPresent() ? path
                : shortestWithout(a, z, this::portReuse, Collections.<GraphEdge>emptySet(), MAX_BRANCHINGS);
    }

    /** The STPs a path passes through, in order: its two ends with both sides of every SDP between. */
    List<StpType> stps(List<GraphEdge> path) {
        List<StpType> stps = new ArrayList<>();
        for (StpPair segment : DijkstraPCE.getIndividualSegments(topology, path)) {
            stps.add(segment.getA());
            stps.add(segment.getZ());
        }
        return stps;
    }

    /** Indexes into stps of each STP that enters a network the path had already left. */
    static List<Integer> reentries(List<StpType> stps) {
        List<Integer> reentries = new ArrayList<>();
        Set<String> left = new HashSet<>();
        for (int i = 1; i < stps.size(); i++) {
            String previous = stps.get(i - 1).getNetworkId().toLowerCase();
            String current = stps.get(i).getNetworkId().toLowerCase();
            if (!current.equals(previous)) {
                left.add(previous);
                if (left.contains(current)) {
                    reentries.add(i);
                }
            }
        }
        return reentries;
    }

    /**
     * Branches on the first conflict of the shortest path: a path without the conflict avoids one of
     * its two sides, so each side is blocked in turn and the shorter result kept. Exact up to budget
     * nested conflicts; a path without conflicts, the usual case, costs one search.
     */
    private Optional<List<GraphEdge>> shortestWithout(StpType a, StpType z,
            Function<List<GraphEdge>, Optional<List<Set<GraphEdge>>>> conflicts, Set<GraphEdge> blocked, int budget) {
        List<GraphEdge> path = shortest(a, z, blocked);
        if (path.isEmpty()) {
            return Optional.empty();
        }
        Optional<List<Set<GraphEdge>>> conflict = conflicts.apply(path);
        if (!conflict.isPresent()) {
            return Optional.of(path);
        }
        if (budget == 0) {
            return Optional.empty();
        }
        return conflict.get().stream()
                .map(side -> shortestWithout(a, z, conflicts, union(blocked, side), budget - 1))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingInt(List::size));
    }

    private List<GraphEdge> shortest(StpType a, StpType z, Set<GraphEdge> blocked) {
        DijkstraShortestPath<GraphVertex, GraphEdge> dijkstra = blocked.isEmpty() ? unblocked
                : blockedSearches.computeIfAbsent(blocked,
                        edges -> new DijkstraShortestPath<>(graph, new Blocking(edges, weights), true));
        List<GraphEdge> path = dijkstra.getPath(vertices.get(a.getId()), vertices.get(z.getId()));
        return path.stream().anyMatch(blocked::contains) ? Collections.<GraphEdge>emptyList() : path;
    }

    /**
     * The first time the path re-enters a network. The sides are the service domain it first had in
     * that network and the one it re-entered: a non-swapping network is one service domain per VLAN,
     * so this is a VLAN change made outside the network.
     */
    private Optional<List<Set<GraphEdge>>> reentry(List<GraphEdge> path) {
        List<StpType> stps = stps(path);
        return reentries(stps).stream().findFirst().map(i -> {
            StpType reentered = stps.get(i);
            StpType firstVisit = stps.stream()
                    .filter(stp -> stp.getNetworkId().equalsIgnoreCase(reentered.getNetworkId()))
                    .findFirst().get();
            return Arrays.asList(incident(firstVisit), incident(reentered));
        });
    }

    /** The first two SDPs on the path that share a port: out and back on one link, which uPAs refuse. */
    private Optional<List<Set<GraphEdge>>> portReuse(List<GraphEdge> path) {
        List<StpType> stps = stps(path);
        List<GraphEdge> sdps = new ArrayList<>();
        path.stream().filter(edge -> edge instanceof SdpEdge).forEach(sdps::add);
        // stps is [start, near 0, far 0, near 1, far 1, ..., end]: STP i is a side of SDP (i - 1) / 2.
        Map<String, Integer> sdpByPort = new HashMap<>();
        for (int i = 1; i + 1 < stps.size(); i++) {
            int sdp = (i - 1) / 2;
            Integer earlier = sdpByPort.putIfAbsent(SimpleStp.getId(stps.get(i).getId()).toLowerCase(), sdp);
            if (earlier != null && earlier != sdp) {
                return Optional.of(Arrays.asList(Collections.singleton(sdps.get(earlier)),
                        Collections.singleton(sdps.get(sdp))));
            }
        }
        return Optional.empty();
    }

    private Set<GraphEdge> incident(StpType stp) {
        return new HashSet<>(graph.getIncidentEdges(vertices.get(stp.getServiceDomain().getId())));
    }

    private static Set<GraphEdge> union(Set<GraphEdge> a, Set<GraphEdge> b) {
        Set<GraphEdge> union = new HashSet<>(a);
        union.addAll(b);
        return union;
    }

    /** Edge weights with the blocked edges made prohibitively heavy; holds only what it needs. */
    private static final class Blocking implements Transformer<GraphEdge, Number> {
        private final Set<GraphEdge> blocked;
        private final Transformer<GraphEdge, Number> weights;

        Blocking(Set<GraphEdge> blocked, Transformer<GraphEdge, Number> weights) {
            this.blocked = blocked;
            this.weights = weights;
        }

        @Override
        public Number transform(GraphEdge edge) {
            return blocked.contains(edge) ? BLOCKED : weights.transform(edge);
        }
    }
}
