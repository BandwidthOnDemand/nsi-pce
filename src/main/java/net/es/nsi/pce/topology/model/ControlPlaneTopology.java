package net.es.nsi.pce.topology.model;

import com.google.common.base.Strings;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SortedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.jaxb.topology.NsaFeatureType;
import net.es.nsi.pce.jaxb.topology.NsaType;
import net.es.nsi.pce.jaxb.topology.PeerRoleEnum;
import net.es.nsi.pce.jaxb.topology.PeersWithType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.pf.graph.NsaEdge;
import net.es.nsi.pce.pf.graph.NsaVertex;
import net.es.nsi.pce.schema.NsiConstants;
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

    public String findNextNsa(String sourceNsa, String destNsa) throws WebApplicationException {
        if (Strings.isNullOrEmpty(sourceNsa)) {
            throw new IllegalArgumentException("findNextNsa: sourceNsa not provided for control plane path finding");
        } else if (Strings.isNullOrEmpty(destNsa)) {
            throw new IllegalArgumentException("findNextNsa: destNsa not provided for control plane path finding");
        }

        // The graph does not handle edges looping back on the same vertex so we
        // have special handling code.  Assume if it is asked for that the NSA
        // is a uPA.
        if (destNsa.equalsIgnoreCase(sourceNsa)) {
            return sourceNsa;
        }

        DijkstraShortestPath<NsaVertex, NsaEdge> alg = new DijkstraShortestPath<>(controlPlane, true);

        Optional<NsaVertex> sourceVertex = Optional.ofNullable(verticies.get(sourceNsa));
        Optional<NsaVertex> destVertex = Optional.ofNullable(verticies.get(destNsa));

        if (!sourceVertex.isPresent() ) {
            log.error("findNextNsa: source NSA " + sourceNsa + " not present in control plane graph.");
            throw Exceptions.noControlPlanePathFound("source NSA " + sourceNsa + " not present in control plane graph.");
        }
        else if (!destVertex.isPresent()) {
            log.error("findNextNsa: destination NSA " + sourceNsa + " not present in control plane graph.");
            throw Exceptions.noControlPlanePathFound("destination NSA " + destNsa + " not present in control plane graph.");
        }

        List<NsaEdge> path;
        try {
            path = alg.getPath(sourceVertex.get(), destVertex.get());
        } catch (Exception ex) {
            log.error("findNextNsa: no control plane path from " + sourceNsa + " to " + destNsa);
            throw Exceptions.noControlPlanePathFound(sourceNsa + " to " + destNsa);
        }

        if (path == null || path.isEmpty()) {
            log.error("findNextNsa: no control plane path from " + sourceNsa + " to " + destNsa);
            throw Exceptions.noControlPlanePathFound(sourceNsa + " to " + destNsa);
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
