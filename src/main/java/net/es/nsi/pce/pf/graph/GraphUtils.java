package net.es.nsi.pce.pf.graph;

import net.es.nsi.pce.pf.graph.SdpEdge;
import net.es.nsi.pce.pf.graph.StpEdge;
import com.google.common.base.Optional;
import edu.uci.ics.jung.graph.Graph;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.es.nsi.pce.jaxb.topology.ResourceRefType;
import net.es.nsi.pce.jaxb.topology.SdpDirectionalityType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.ServiceDomainType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.pf.PfUtils;
import net.es.nsi.pce.pf.route.StpTypeBundle;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for populating the Jung Graph with NSI objects.
 * 
 * @author hacksaw
 */
public class GraphUtils {
    private final static Logger log = LoggerFactory.getLogger(GraphUtils.class);

    public static void addStpVerticies(Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, StpTypeBundle bundle) {
        for (StpType stp : bundle.values()) {
            StpVertex srcVertex = new StpVertex(stp.getId(), stp);
            graph.addVertex(srcVertex);
            verticies.put(srcVertex.getId(), srcVertex);
        }
    }

    public static void addServiceDomainVerticies(Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, Collection<ServiceDomainType> serviceDomains) {
        // Add ServiceDomains as verticies.
        for (ServiceDomainType serviceDomain : serviceDomains) {
            ServiceDomainVertex vertex = new ServiceDomainVertex(serviceDomain.getId(), serviceDomain);
            graph.addVertex(vertex);
            verticies.put(vertex.getId(), vertex);
        }
    }

    public static void linkStpAndServiceDomainVerticies(NsiTopology topology, Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, StpTypeBundle bundle, Collection<ServiceDomainType> serviceDomains) {
        for (StpType stp : bundle.values()) {
            ServiceDomainType serviceDomain = PfUtils.getServiceDomainOrFail(topology, stp);
            StpEdge edge = new StpEdge(stp.getId(), stp, serviceDomain);
            GraphVertex vertex = verticies.get(stp.getId());
            graph.addEdge(edge, vertex, verticies.get(serviceDomain.getId()));
        }
    }

    public static void addSdpEdges(NsiTopology topology, Graph<GraphVertex, GraphEdge> graph, Map<String, GraphVertex> verticies, Set<String> exclusionSdp) {
        // We only do bidirectional path finding at this point so add the
        // bidirectional SDP as edges.
        for (SdpType sdp : topology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                Optional<ResourceRefType> aServiceDomainRef = Optional.fromNullable(sdp.getDemarcationA().getServiceDomain());
                Optional<ResourceRefType> zServiceDomainRef = Optional.fromNullable(sdp.getDemarcationZ().getServiceDomain());

                if (!aServiceDomainRef.isPresent()) {
                    log.error("Missing service domain for demarcationA sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationA().getStp().getId());
                }
                else if (!zServiceDomainRef.isPresent()) {
                    log.error("Missing service domain for demarcationZ sdpId=" + sdp.getId() + " and stpId=" + sdp.getDemarcationZ().getStp().getId());
                }
                else if (exclusionSdp.contains(sdp.getId())) {
                    log.debug("Omitting SDP sdpId=" + sdp.getId());
                }
                else {
                    SdpEdge sdpEdge = new SdpEdge(sdp.getId(), sdp);
                    graph.addEdge(sdpEdge, verticies.get(aServiceDomainRef.get().getId()), verticies.get(zServiceDomainRef.get().getId()));
                }
            }
        }
    }
}
