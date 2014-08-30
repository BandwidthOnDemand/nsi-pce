package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SortedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaFeatureType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author hacksaw
 */
public class ResolvePCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, NsaVertex> verticies = new HashMap<>();

    @Override
    public PCEData apply(PCEData pceData) {
        NsiTopology topology = pceData.getTopology();
        Graph<NsaVertex, NsaEdge> controlPlane = graph(topology);

        for (PathSegment segment : pceData.getPath().getPathSegments()) {
            // Find the NSA managing this segment.
            String networkId = segment.getStpPair().getA().getNetworkId();
            NetworkType network = topology.getNetworkById(networkId);

            // Find a path to this NSA through the control plane.
            String nextNsa = findNextNsa(controlPlane, topology.getLocalNsaId(), network.getNsa().getId());
            segment.setNsaId(nextNsa);
            Optional<String> providerUrl = topology.getProviderUrl(nextNsa);
            if (providerUrl.isPresent()) {
              segment.setCsProviderURL(providerUrl.get());
            }
        }

        return pceData;
    }

    private String findNextNsa(Graph<NsaVertex, NsaEdge> controlPlane, String sourceNsa, String destNsa) {
        // The graph does not handle edges looping back on the same vertex so we
        // have special handling code.  Assume if it is asked for that the NSA
        // is a uPA.
        if (sourceNsa.equalsIgnoreCase(destNsa)) {
            return sourceNsa;
        }

        DijkstraShortestPath<NsaVertex, NsaEdge> alg = new DijkstraShortestPath<>(controlPlane, true);

        List<NsaEdge> path;
        try {
            path = alg.getPath(verticies.get(sourceNsa), verticies.get(destNsa));
        } catch (IllegalArgumentException ex) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_CONTROLPLANE_PATH_FOUND, sourceNsa + " to " + destNsa);
            log.error(error, ex);
            throw new IllegalArgumentException(error);
        }

        if (path == null || path.isEmpty()) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_CONTROLPLANE_PATH_FOUND, sourceNsa + " to " + destNsa);
            log.error(error);
            return destNsa;
        }

        return path.get(0).getDestinationNsa().getId();
    }

    private boolean isAG(List<NsaFeatureType> features) {
        if (features == null) {
            return false;
        }

        for (NsaFeatureType feature : features) {
            if (NsiConstants.NSI_CS_AGG.equalsIgnoreCase(feature.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean isPA(List<NsaFeatureType> features) {
        if (features == null) {
            return false;
        }

        for (NsaFeatureType feature : features) {
            if (NsiConstants.NSI_CS_UPA.equalsIgnoreCase(feature.getType()) ||
                    NsiConstants.NSI_CS_AGG.equalsIgnoreCase(feature.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean isUPA(List<NsaFeatureType> features) {
        if (features == null) {
            return false;
        }

        for (NsaFeatureType feature : features) {
            if (NsiConstants.NSI_CS_UPA.equalsIgnoreCase(feature.getType())) {
                return true;
            }
        }

        return false;
    }

    private Graph<NsaVertex, NsaEdge> graph(NsiTopology topology) throws IllegalArgumentException, RuntimeException {
        Graph<NsaVertex, NsaEdge> graph = new SortedSparseMultigraph<>();
        verticies.clear();

        // Add all NSA as verticies.
        for (NsaType nsa : topology.getNsas()) {
            NsaVertex vertex = new NsaVertex(nsa.getId(), nsa);
            graph.addVertex(vertex);
            verticies.put(vertex.getId(), vertex);
        }

        // We add unidirectional edges to the graph based on the available
        // peerings between NSA.  An aggregator NSA has both inbound and outbound
        // edges (RA and PA roles), while a uPA (PA role) has only inbound edges.
        // When adding edges only add the outbound from each vertex.  We will
        // eventually have them all added.
        for (NsaType nsa : topology.getNsas()) {
            NsaVertex sourceNsa = verticies.get(nsa.getId());

            if (isAG(nsa.getFeature())) {
                for (String peer : nsa.getPeersWith()) {
                    NsaVertex peerNsa = verticies.get(peer);
                    if (peerNsa == null) {
                        log.error("NSA id=" + nsa.getId() + " has invalid peersWith id=" + peer);
                        continue;
                    }
                    if (isPA(peerNsa.getNsa().getFeature())) {
                        // We add an outgoing edge for this NSA.
                        Pair<NsaVertex> pair = new Pair<>(sourceNsa, peerNsa);
                        NsaEdge edge = new NsaEdge(nsa, peerNsa.getNsa());
                        graph.addEdge(edge, pair, EdgeType.DIRECTED);
                    }
                }
            }
        }

        return graph;
    }
}
