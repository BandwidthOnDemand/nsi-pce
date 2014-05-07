package net.es.nsi.pce.pf;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections15.Transformer;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.pf.api.NsiError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraints;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.model.LabelUtilities;
import net.es.nsi.pce.topology.model.SimpleLabel;
import net.es.nsi.pce.topology.model.SimpleStp;
import net.es.nsi.pce.topology.model.SimpleStpPair;
import net.es.nsi.pce.topology.model.SimpleStpPairCollection;

/**
 * Main path computation class using Dijkstra's shortest path on an NSI
 * topology model.
 *
 * @author hacksaw
 */
public class DijkstraPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private NsiTopology nsiTopology;
    private Map<String, DijkstraVertex> verticies = new HashMap<>();

    /**
     * This path computation module supports pathfinding for the P2PS service
     * specification restricted to bidirectional symmetricPath services with
     * two stpId specified in the request.
     *
     * This module does not currently use the following parameters:
     *    - startTime
     *    - endTime
     *    - capacity
     *    - ero
     */
    @Override
    public PCEData apply(PCEData pceData) {

        // Parse out the constraints this PCE module supports.
        Constraints constraints = new Constraints(pceData.getConstraints());

        // Determine directionality of service request, default to bidirectional if not present.
        DirectionalityType directionality = DirectionalityType.BIDIRECTIONAL;
        StringAttrConstraint directionalityConstraint = constraints.getStringAttrConstraint(Point2Point.DIRECTIONALITY);
        if (directionalityConstraint != null) {
            directionality = DirectionalityType.valueOf(directionalityConstraint.getValue());
        }

        // Determine path symmetry.
        boolean symmetricPath = true;
        BooleanAttrConstraint symmetricPathConstraint = constraints.getBooleanAttrConstraint(Point2Point.SYMMETRICPATH);
        if (symmetricPathConstraint != null) {
            symmetricPath = Boolean.valueOf(symmetricPathConstraint.getValue());
        }

        // Get source stpId.
        StringAttrConstraint sourceStp = constraints.getStringAttrConstraint(Point2Point.SOURCESTP);
        if (sourceStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2Point.NAMESPACE, Point2Point.SOURCESTP));
        }
        String srcStpId = sourceStp.getValue();

        // Get destination stpId.
        StringAttrConstraint destStp = constraints.getStringAttrConstraint(Point2Point.DESTSTP);
        if (destStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2Point.NAMESPACE, Point2Point.DESTSTP));
        }
        String dstStpId = destStp.getValue();

        // Get the topology model used for routing.
        nsiTopology = pceData.getTopology();

        log.debug("******* DijkstraPCE localNsaId " + nsiTopology.getLocalNsaId());
        for (String networkId : nsiTopology.getNetworkIds()) {
            log.debug("******* localNetworkId=" + networkId);
        }

        // Look up the STP within our model matching the request.
        SimpleStp srcId = new SimpleStp(srcStpId);
        StpType srcStp = nsiTopology.getStp(srcId.getId());

        SimpleStp dstId = new SimpleStp(dstStpId);
        StpType dstStp = nsiTopology.getStp(dstId.getId());

        // TODO: If we decide to allow blind routing to a network then remove
        // these tests for a null STP.
        if (srcStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.SOURCESTP, srcStpId));
        }
        else if (dstStp == null) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.DESTSTP, dstStpId));
        }

        // Verify the specified STP are of the correct type for the request.
        if (directionality == DirectionalityType.UNIDIRECTIONAL) {
             if (srcStp.getType() != StpDirectionalityType.INBOUND &&
                     srcStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2Point.SOURCESTP, srcStpId));
            }

            if (dstStp.getType() != StpDirectionalityType.INBOUND &&
                     dstStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST, Point2Point.DESTSTP, dstStpId));
            }
        }
        else {
            if (srcStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2Point.SOURCESTP, srcStpId));
            }

            if (dstStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST, Point2Point.DESTSTP, dstStpId));
            }
        }


        // Verify that the requested VLAN ranges on the source and destination
        // STP are valid within the context of the request.
        Set<SimpleLabel> srcRequestLabels = srcId.getLabels();
        Set<SimpleLabel> sourceStpLabels = LabelUtilities.getLabelsAsSet(srcStp.getLabels());
        Set<SimpleLabel> srcSet;
        if (srcRequestLabels.isEmpty() && sourceStpLabels.isEmpty()) {
            // We have no labels in the request or the STP so we don't have to
            // worry about one.
            srcSet = null;
        }
        else if (srcRequestLabels.isEmpty()) {
            // This means any label on the STP can be used in the request.
            srcSet = sourceStpLabels;
        }
        else {
            // Find the intersection of the label sets.
            srcSet = Sets.intersection(srcRequestLabels, sourceStpLabels);
            if (srcSet.isEmpty()) {
                // There are no matching labels so return an error,
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_LABEL_UNSUPPORTED, Point2Point.SOURCESTP, srcStpId));
            }
        }

        Set<SimpleLabel> dstRequestLabels = dstId.getLabels();
        Set<SimpleLabel> dstStpLabels = LabelUtilities.getLabelsAsSet(dstStp.getLabels());
        Set<SimpleLabel> dstSet;
        if (dstRequestLabels.isEmpty() && dstStpLabels.isEmpty()) {
            // We have no labels in the request or the STP so we don't have to
            // worry about one.
            dstSet = null;
        }
        else if (dstRequestLabels.isEmpty()) {
            // This means any label on the STP can be used in the request.
            dstSet = dstStpLabels;
        }
        else {
            // Find the intersection of the label sets.
            dstSet = Sets.intersection(dstRequestLabels, dstStpLabels);
            if (dstSet.isEmpty()) {
                // There are no matching labels so return an error,
                throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_LABEL_UNSUPPORTED, Point2Point.DESTSTP, dstStpId));
            }
        }

        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<DijkstraVertex, DijkstraEdge> graph = getBidirectionGraph(srcStp, srcSet, dstStp, dstSet);

        Transformer<DijkstraEdge,Number> trans = new DijkstraTrasnsformer(pceData);

        DijkstraShortestPath<DijkstraVertex, DijkstraEdge> alg = new DijkstraShortestPath<>(graph, trans, false);

        SimpleStpPairCollection pairs = new SimpleStpPairCollection(srcId.getId(), srcSet, dstId.getId(), dstSet);
        List<DijkstraEdge> path = null;
        SimpleStpPair pair;
        Iterator<SimpleStpPair> iterator = pairs.iterator();
        while (iterator.hasNext()) {
            pair = iterator.next();

            DijkstraVertex srcVertex = verticies.get(pair.getSrc().getStpId());
            DijkstraVertex dstVertex = verticies.get(pair.getDst().getStpId());

            try {
                path = alg.getPath(srcVertex, dstVertex);
                log.debug("Path computation completed with " + path.size() + " SDP returned for srcId=" + pair.getSrc().getStpId() + ", dstId=" + pair.getDst().getStpId());

                if (!path.isEmpty()) {
                    break;
                }
            } catch (IllegalArgumentException ex) {
                String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "DijkstraPCE", ex.getMessage());
                log.error(error, ex);
                throw new IllegalArgumentException(error);
            }
        }

        // Check to see if there is a valid path.
        if (path == null || path.isEmpty()) {
            String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "DijkstraPCE", "No path found using provided criteria");
            throw new RuntimeException(error);
        }

        // Make a copy of the original comstraints.  These will be applied to
        // individual path segment results.
        Constraints segmentConstraints = new Constraints(pceData.getConstraints());
        segmentConstraints.removeStringAttrConstraint(Point2Point.SOURCESTP);
        segmentConstraints.removeStringAttrConstraint(Point2Point.DESTSTP);

        List<StpPair> segments = crap(pair, path, nsiTopology);
        for (int i = 0; i < segments.size(); i++) {
            StpPair pair = segments.get(i);

            log.debug("Pair: " + pair.getA().getId() + " -- " + pair.getZ().getId());

            Constraints cons = new Constraints(segmentConstraints);
            PathSegment pathSegment = new PathSegment(pair);
            pathSegment.setConstraints(cons);
            pceData.getPath().getPathSegments().add(i, pathSegment);
        }

        return pceData;
    }

    private Graph<DijkstraVertex, DijkstraEdge> getBidirectionGraph(StpType aStp, Set<SimpleLabel> aLabels, StpType zStp, Set<SimpleLabel> zLabels) {
        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<DijkstraVertex, DijkstraEdge> graph = new SparseMultigraph<>();

        // Add all the Service Domains as verticies.
        processServiceDomains(graph);

        // Add bidirectional SDP as edges and their connecting STP as verticies.
        processSdp(graph);

        // Now we add the requested source and destination STP to the graph.
        // There may be more than one.
        processStp(graph, aStp, aLabels);
        processStp(graph, zStp, zLabels);

        return graph;
    }

    private void processServiceDomains(Graph<DijkstraVertex, DijkstraEdge> graph) {
        // Add ServiceDomains as verticies.
        for (ServiceDomainType serviceDomain : nsiTopology.getServiceDomains()) {
            log.debug("Adding Vertex: " + serviceDomain.getId());
            if (serviceDomain.isLabelSwapping()) {
                // This domain can interconnect any member STP.
                DijkstraVertex sdVertex = new DijkstraVertex(serviceDomain.getId(), serviceDomain);
                graph.addVertex(sdVertex);
                verticies.put(sdVertex.getId(), sdVertex);
            }
            else {
                // We must expand this domain into one per label value (vlan).
                Set<SimpleLabel> labels = new HashSet<>();
                for (ResourceRefType stpRef : serviceDomain.getBidirectionalStp()) {
                    StpType stp = nsiTopology.getStp(stpRef.getId());
                    Set<SimpleLabel> labelsAsSet = LabelUtilities.getLabelsAsSet(stp.getLabels());
                    labels.addAll(labelsAsSet);
                }

                log.debug("processServiceDomains: found the following labels...");
                for (SimpleLabel label : labels) {
                    log.debug(label.getType() + "=" + label.getValue());

                    String sdId = LabelUtilities.getServiceDomainId(serviceDomain, label);
                    DijkstraVertex sdVertex = new DijkstraVertex(sdId, serviceDomain);
                    graph.addVertex(sdVertex);
                    verticies.put(sdVertex.getId(), sdVertex);
                }
            }
        }
    }

    private void processSdp(Graph<DijkstraVertex, DijkstraEdge> graph) {
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                // Get the Service Domains on the ends of the SDP.
                ServiceDomainType aServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
                ServiceDomainType zServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());

                // If both Service Domains can label swap then we only need one
                // edge for the SDP, however, if one end cannot then we need to
                // explode the SDP into an edge per label value.
                if (aServiceDomain.isLabelSwapping() && zServiceDomain.isLabelSwapping()) {
                    // Both ends support label swapping.
                    log.debug("getBidirectionGraph: single SDP required between " + aServiceDomain.getId() + " and " + zServiceDomain.getId());
                    convertSdp(graph, sdp);
                }
                else if (!aServiceDomain.isLabelSwapping() && zServiceDomain.isLabelSwapping()) {
                    // A end service domain support does not support label swapping.
                    log.debug("getBidirectionGraph: single SDP required between " + aServiceDomain.getId() + " and " + zServiceDomain.getId());
                    convertSdpExplodeStpA(graph, sdp);
                }
                else if (aServiceDomain.isLabelSwapping() && !zServiceDomain.isLabelSwapping()) {
                    // Z end service domain support does not support label swapping.
                    convertSdpExplodeStpZ(graph, sdp);
                }
                else if (!aServiceDomain.isLabelSwapping() && !zServiceDomain.isLabelSwapping()) {
                    // Neither end support does not support label swapping.
                    convertSdpExplodeSdps(graph, sdp);
                }
            }
        }
    }

    /**
     * Converts an SDP terminated by two labelSwapping service domains into
     * graph elements.
     *
     * @param graph
     * @param verticies
     * @param nsiTopology
     * @param sdp
     * @param serviceDomains List of service domains used to track which have already been processed.
     */

    private void convertSdp(Graph<DijkstraVertex, DijkstraEdge> graph, SdpType sdp) {
        // Get references to the service domains and corresponding verticies.
        ServiceDomainType aServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
        DijkstraVertex aServiceDomainVertex = verticies.get(aServiceDomain.getId());

        ServiceDomainType zServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());
        DijkstraVertex zServiceDomainVertex = verticies.get(zServiceDomain.getId());

        // Now we add the edge STP and connect them to their service domains.
        // These are new verticies since the STP have not been seen before.
        StpType aStp = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
        DijkstraVertex aStpVertex = new DijkstraVertex(aStp.getId(), aStp);
        verticies.put(aStpVertex.getId(), aStpVertex);
        DijkstraEdge aEdge = new DijkstraEdge(aStp, aServiceDomain);
        graph.addEdge(aEdge, aServiceDomainVertex, aStpVertex);

        StpType zStp = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());
        DijkstraVertex zStpVertex = new DijkstraVertex(zStp.getId(), zStp);
        verticies.put(zStpVertex.getId(), zStpVertex);
        DijkstraEdge zEdge = new DijkstraEdge(zStp, zServiceDomain);
        graph.addEdge(zEdge, zStpVertex, zServiceDomainVertex);

        // Lastly we link the two STP on either edge of the SDP.
        DijkstraEdge sdpEdge = new DijkstraEdge(sdp);
        graph.addEdge(sdpEdge, aStpVertex, zStpVertex);
    }

    private void convertSdpExplodeStpA(Graph<DijkstraVertex, DijkstraEdge> graph, SdpType sdp) {
        // Get base STP involved in this SDP.
        StpType aStp = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
        StpType zStp = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());

        // SDP only get created between STP with the same label values.
        Set<SimpleLabel> aStpSet = LabelUtilities.getLabelsAsSet(aStp.getLabels());
        Set<SimpleLabel> zStpSet = LabelUtilities.getLabelsAsSet(zStp.getLabels());
        SetView<SimpleLabel> intersection = Sets.intersection(aStpSet, zStpSet);

        // Get base Service Domains connected at each end of this SDP.
        ServiceDomainType aServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
        ServiceDomainType zServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());

        // Create the zSTP and connect it to the zServiceDomain.
        DijkstraVertex zStpVertex = new DijkstraVertex(zStp.getId(), zStp);
        DijkstraVertex zServiceDomainVertex = verticies.get(zServiceDomain.getId());
        verticies.put(zStpVertex.getId(), zStpVertex);
        DijkstraEdge zEdge = new DijkstraEdge(zStp, zServiceDomain);
        graph.addEdge(zEdge, zStpVertex, zServiceDomainVertex);

        // Expand STP A into individul STPs enumerated based on label values.
        for (SimpleLabel label : intersection) {
            // Create a new aSTP for this specific label.
            String stpId = LabelUtilities.getStpId(aStp, label);
            log.debug("convertSdpExplodeStpA: adding vertex stpId=" + stpId);
            DijkstraVertex aStpVertex = new DijkstraVertex(stpId, aStp, label);
            verticies.put(aStpVertex.getId(), aStpVertex);

            // Get the Service Domain associated with this STP label and
            // connect it to the aSTP.
            String serviceDomainId = LabelUtilities.getServiceDomainId(aServiceDomain, label);
            log.debug("convertSdpExplodeStpA: connecting to serviceDomainId=" + serviceDomainId);
            DijkstraVertex aServiceDomainVertex = verticies.get(serviceDomainId);
            DijkstraEdge aEdge = new DijkstraEdge(stpId, aStp, label, aServiceDomain);
            graph.addEdge(aEdge, aServiceDomainVertex, aStpVertex);

            // Now connect this aSTP to the single zSTP using a label specific SDP.
            String sdpId = LabelUtilities.getSdpId(sdp, label);
            DijkstraEdge sdpEdge = new DijkstraEdge(sdpId, sdp, label);
            graph.addEdge(sdpEdge, aStpVertex, zStpVertex);
        }
    }

    private void convertSdpExplodeStpZ(Graph<DijkstraVertex, DijkstraEdge> graph, SdpType sdp) {
        // Get base STP involved in this SDP.
        StpType aStp = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
        StpType zStp = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());

        // SDP only get created between STP with the same label values.
        Set<SimpleLabel> aStpSet = LabelUtilities.getLabelsAsSet(aStp.getLabels());
        Set<SimpleLabel> zStpSet = LabelUtilities.getLabelsAsSet(zStp.getLabels());
        SetView<SimpleLabel> intersection = Sets.intersection(aStpSet, zStpSet);

        // Get base Service Domains connected at each end of this SDP.
        ServiceDomainType aServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
        ServiceDomainType zServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());

        // Create the aSTP and connect it to the aServiceDomain.
        DijkstraVertex aStpVertex = new DijkstraVertex(aStp.getId(), aStp);
        DijkstraVertex aServiceDomainVertex = verticies.get(aServiceDomain.getId());
        verticies.put(aStpVertex.getId(), aStpVertex);
        DijkstraEdge aEdge = new DijkstraEdge(aStp, aServiceDomain);
        graph.addEdge(aEdge, aStpVertex, aServiceDomainVertex);

        // Expand STP A into individul STPs enumerated based on label values.
        for (SimpleLabel label : intersection) {
            // Create a new aSTP for this specific label.
            String stpId = LabelUtilities.getStpId(zStp, label);
            log.debug("convertSdpExplodeStpZ: adding vertex stpId=" + stpId);
            DijkstraVertex zStpVertex = new DijkstraVertex(stpId, zStp, label);
            verticies.put(zStpVertex.getId(), zStpVertex);

            // Get the Service Domain associated with this STP label and
            // connect it to the aSTP.
            String serviceDomainId = LabelUtilities.getServiceDomainId(zServiceDomain, label);
            log.debug("convertSdpExplodeStpZ: connecting to serviceDomainId=" + serviceDomainId);
            DijkstraVertex zServiceDomainVertex = verticies.get(serviceDomainId);
            DijkstraEdge zEdge = new DijkstraEdge(stpId, zStp, label, zServiceDomain);
            graph.addEdge(zEdge, zServiceDomainVertex, zStpVertex);

            // Now connect this aSTP to the single zSTP using a label specific SDP.
            String sdpId = LabelUtilities.getSdpId(sdp, label);
            DijkstraEdge sdpEdge = new DijkstraEdge(sdpId, sdp, label);
            graph.addEdge(sdpEdge, zStpVertex, zStpVertex);
        }
    }

    private void convertSdpExplodeSdps(Graph<DijkstraVertex, DijkstraEdge> graph, SdpType sdp) {
        // Get base STP involved in this SDP.
        StpType aStp = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
        StpType zStp = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());

        // SDP only get created between STP with the same label values.
        Set<SimpleLabel> aStpSet = LabelUtilities.getLabelsAsSet(aStp.getLabels());
        Set<SimpleLabel> zStpSet = LabelUtilities.getLabelsAsSet(zStp.getLabels());
        SetView<SimpleLabel> intersection = Sets.intersection(aStpSet, zStpSet);

        // Get base Service Domains connected at each end of this SDP.
        ServiceDomainType aServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
        ServiceDomainType zServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());

        // Expand STP A into individul STPs enumerated based on label values.
        for (SimpleLabel label : intersection) {
            // Create a new aSTP for this specific label.
            String aStpId = LabelUtilities.getStpId(aStp, label);
            log.debug("convertSdpExplodeSdps: adding vertex aStpId=" + aStpId);
            DijkstraVertex aStpVertex = new DijkstraVertex(aStpId, aStp, label);
            verticies.put(aStpVertex.getId(), aStpVertex);

            // Get the Service Domain associated with this STP label and
            // connect it to the aSTP.
            String aServiceDomainId = LabelUtilities.getServiceDomainId(aServiceDomain, label);
            log.debug("convertSdpExplodeSdps: connecting to aServiceDomainId=" + aServiceDomainId);
            DijkstraVertex aServiceDomainVertex = verticies.get(aServiceDomainId);
            DijkstraEdge aEdge = new DijkstraEdge(aStpId, aStp, label, aServiceDomain);
            graph.addEdge(aEdge, aServiceDomainVertex, aStpVertex);

            // Create a new zSTP for this specific label.
            String zStpId = LabelUtilities.getStpId(zStp, label);
            log.debug("convertSdpExplodeSdps: adding vertex zStpId=" + zStpId);
            DijkstraVertex zStpVertex = new DijkstraVertex(zStpId, zStp, label);
            verticies.put(zStpVertex.getId(), zStpVertex);

            // Get the Service Domain associated with this STP label and
            // connect it to the zSTP.
            String zServiceDomainId = LabelUtilities.getServiceDomainId(zServiceDomain, label);
            log.debug("convertSdpExplodeSdps: connecting to zServiceDomainId=" + zServiceDomainId);
            DijkstraVertex zServiceDomainVertex = verticies.get(zServiceDomainId);
            DijkstraEdge zEdge = new DijkstraEdge(zStpId, zStp, label, zServiceDomain);
            graph.addEdge(zEdge, zServiceDomainVertex, zStpVertex);

            // Now connect the aSTP to the zSTP using a label specific SDP.
            String sdpId = LabelUtilities.getSdpId(sdp, label);
            DijkstraEdge sdpEdge = new DijkstraEdge(sdpId, sdp, label);
            graph.addEdge(sdpEdge, aStpVertex, zStpVertex);
        }
    }

    private void processStp(Graph<DijkstraVertex, DijkstraEdge> graph, StpType stp, Set<SimpleLabel> labels) {
        // Determine the service domain associated with this STP.
        ServiceDomainType serviceDomain = nsiTopology.getServiceDomain(stp.getServiceDomain().getId());
        if (serviceDomain.isLabelSwapping()) {
            // Our service domain supports label swapping so there will only be
            // one vertex in the graph.
            DijkstraVertex serviceDomainVertex = verticies.get(serviceDomain.getId());
            for (SimpleLabel label : labels) {
                // Create a new STP for this specific label.
                String stpId = LabelUtilities.getStpId(stp, label);
                log.debug("processStp: adding vertex stpId=" + stpId);
                DijkstraVertex stpVertex = new DijkstraVertex(stpId, stp, label);
                verticies.put(stpVertex.getId(), stpVertex);

                DijkstraEdge edge = new DijkstraEdge(stpId, stp, label, serviceDomain);
                graph.addEdge(edge, serviceDomainVertex, stpVertex);
            }
        }
        else {
            // We will have one service domain per STP label.
            for (SimpleLabel label : labels) {
                String serviceDomainId = LabelUtilities.getServiceDomainId(serviceDomain, label);
                DijkstraVertex serviceDomainVertex = verticies.get(serviceDomainId);

                // Create a new STP for this specific label.
                String stpId = LabelUtilities.getStpId(stp, label);
                log.debug("processStp: adding vertex stpId=" + stpId);
                DijkstraVertex stpVertex = new DijkstraVertex(stpId, stp, label);
                verticies.put(stpVertex.getId(), stpVertex);

                DijkstraEdge edge = new DijkstraEdge(stpId, stp, label, serviceDomain);
                graph.addEdge(edge, serviceDomainVertex, stpVertex);
            }
        }
    }

    protected List<StpPair> pullIndividualSegmentsOut(StpType srcStp, StpType dstStp, List<SdpType> path, NsiTopology nsiTopology) {
        List<StpPair> segments = new ArrayList<>();

        StpType start = srcStp;
        for (SdpType edge: path) {
            log.debug("--- Edge: " + edge.getId());
            StpType stpA = nsiTopology.getStp(edge.getDemarcationA().getStp().getId());
            StpType stpZ = nsiTopology.getStp(edge.getDemarcationZ().getStp().getId());

            StpPair pathPair;
            if (start.getNetworkId().equalsIgnoreCase(stpA.getNetworkId())) {
                pathPair = new StpPair(start, stpA);
                start = stpZ;
            }
            else {
                pathPair = new StpPair(start, stpZ);
                start = stpA;
            }
            segments.add(pathPair);
        }

        segments.add(new StpPair(start, dstStp));

        return segments;
    }

    protected List<StpPair> crap(SimpleStpPair pair, List<DijkstraEdge> path, NsiTopology nsiTopology) {
        List<Stp> segments = new ArrayList<>();
        List<StpPair> segments = new ArrayList<>();

        StpType start = srcStp;
        for (DijkstraEdge edge: path) {
            log.debug("--- Edge: " + edge.getId());
            if (edge.getType() == DijkstraEdgeType.SDP) {

            }
            else if (edge.getType() == DijkstraEdgeType.SDP) {

            }
            else {
                String error = NsiError.getFindPathErrorString(NsiError.NO_PATH_FOUND, "DijkstraPCE", "Unknown edge type " + edge.getType().name());
                log.error(error);
                throw new IllegalArgumentException(error);
            }

            StpType stpA = nsiTopology.getStp(edge.getDemarcationA().getStp().getId());
            StpType stpZ = nsiTopology.getStp(edge.getDemarcationZ().getStp().getId());

            StpPair pathPair;
            if (start.getNetworkId().equalsIgnoreCase(stpA.getNetworkId())) {
                pathPair = new StpPair(start, stpA);
                start = stpZ;
            }
            else {
                pathPair = new StpPair(start, stpZ);
                start = stpA;
            }
            segments.add(pathPair);
        }

        segments.add(new StpPair(start, dstStp));

        return segments;
    }
}
