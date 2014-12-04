package net.es.nsi.pce.topology.model;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SortedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.es.nsi.pce.pf.NsaEdge;
import net.es.nsi.pce.pf.NsaVertex;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.topology.jaxb.NsaFeatureType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.PeerRoleEnum;
import net.es.nsi.pce.topology.jaxb.PeersWithType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models control plane topology based on the NSA description document's
 * peersWith element.
 *
 * @author hacksaw
 */
public class ControlPlaneTopology {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, NsaVertex> verticies;
    private Graph<NsaVertex, NsaEdge> controlPlane;

    public ControlPlaneTopology(NsiTopology tp) {
        graph(tp);
    }

    public String findNextNsa(String sourceNsa, String destNsa) {
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

    private boolean isRA(List<NsaFeatureType> features) {
        if (features == null) {
            return false;
        }

        for (NsaFeatureType feature : features) {
            if (NsiConstants.NSI_CS_URA.equalsIgnoreCase(feature.getType()) ||
                    NsiConstants.NSI_CS_AGG.equalsIgnoreCase(feature.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean isURA(List<NsaFeatureType> features) {
        if (features == null) {
            return false;
        }

        for (NsaFeatureType feature : features) {
            if (NsiConstants.NSI_CS_URA.equalsIgnoreCase(feature.getType())) {
                return true;
            }
        }

        return false;
    }

    private void graph(NsiTopology topology) throws IllegalArgumentException, RuntimeException {
        controlPlane = new SortedSparseMultigraph<>();
        verticies = new HashMap<>();

        // Add all NSA as verticies.
        for (NsaType nsa : topology.getNsas()) {
            log.debug("Adding vertex " + nsa.getId());
            NsaVertex vertex = new NsaVertex(nsa.getId(), nsa);
            controlPlane.addVertex(vertex);
            verticies.put(vertex.getId(), vertex);
        }

        // We add unidirectional edges to the graph based on the available
        // peerings between NSA.  An aggregator NSA has both inbound and outbound
        // edges (RA and PA roles), while a uPA (PA role) has only inbound edges.
        // When adding edges only add the outbound from each vertex.  We will
        // eventually have them all added.
        for (NsaType nsa : topology.getNsas()) {
            NsaVertex sourceNsa = verticies.get(nsa.getId());

            if (isRA(nsa.getFeature())) {
                for (PeersWithType peer : nsa.getPeersWith()) {
                    if (peer.getRole() == PeerRoleEnum.RA) {
                        NsaVertex destNsa = verticies.get(peer.getId());
                        if (destNsa == null) {
                            log.error("NSA id=" + nsa.getId() + " has invalid peersWith id=" + peer.getId());
                            continue;
                        }
                        if (isPA(destNsa.getNsa().getFeature())) {
                            log.debug("Adding edge from " + sourceNsa.getId() + " to " + destNsa.getId());
                            // We add an outgoing edge for this NSA.
                            Pair<NsaVertex> pair = new Pair<>(sourceNsa, destNsa);
                            NsaEdge edge = new NsaEdge(nsa, destNsa.getNsa());
                            controlPlane.addEdge(edge, pair, EdgeType.DIRECTED);
                        }
                    }
                }
            }
        }
    }
}
