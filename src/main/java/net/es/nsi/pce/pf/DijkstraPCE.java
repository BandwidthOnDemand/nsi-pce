package net.es.nsi.pce.pf;


import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.PathEndpoints;
import net.es.nsi.pce.pf.api.topo.JsonTopologyProvider;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;

import java.util.List;

public class DijkstraPCE implements PCEModule {

    public PCEData apply(PCEData pceData) throws Exception {
        PathEndpoints pe = null;
        for (Constraint c : pceData.getConstraints()) {
            if (c instanceof PathEndpoints) {
                pe = (PathEndpoints) c;
            }
        }

        Topology topo = pceData.getTopo();

        Network srcNet = topo.getNetwork(pe.getSrcNetwork());
        Network dstNet = topo.getNetwork(pe.getDstNetwork());
        if (srcNet == null) {
            throw new Exception("unknown src network "+pe.getSrcNetwork());
        } else if (dstNet == null) {
            throw new Exception("unknown dst network "+pe.getDstNetwork());
        }
        Stp srcStp = srcNet.getStp(pe.getSrcLocal());
        Stp dstStp = dstNet.getStp(pe.getDstLocal());

        if (srcStp == null) {
            throw new Exception("unknown src stp "+pe.getSrcLocal());
        } else if (dstStp == null) {
            throw new Exception("unknown dst stp "+pe.getDstLocal());
        }


        DirectedSparseMultigraph<Stp, String> g = new DirectedSparseMultigraph<Stp, String>();

        for (String netId : topo.getNetworkIds()) {
            System.out.println("edges for network "+netId);
            Network net = topo.getNetwork(netId);
            Stp networkVertex = new Stp();
            networkVertex.network = net;
            networkVertex.localId = netId;

            for (String stpId : net.getStpIds()) {
                Stp stp = net.getStp(stpId);
                Stp remote = stp.remote;
                String netEdge      = "  "+stp.localId+" -- "+networkVertex.localId;
                String netEdgeInv   = "  "+networkVertex.localId+" -- "+stp.localId;

                g.addEdge(netEdge,      stp, networkVertex, EdgeType.DIRECTED);
                g.addEdge(netEdgeInv,   networkVertex, stp, EdgeType.DIRECTED);

                if (remote != null) {
                    String remEdge      = "  "+stp.localId+" -- "+remote.localId;
                    g.addEdge(remEdge,      stp, stp.remote, EdgeType.DIRECTED);
                    System.out.println(remEdge);
                }

                System.out.println(netEdge);
                System.out.println(netEdgeInv);
            }
        }

        DijkstraShortestPath<Stp, String> alg = new DijkstraShortestPath(g);
        List<String> l = alg.getPath(srcStp, dstStp);
        System.out.println("**** PATH ****");
        System.out.println(l.toString());


        return pceData;
    }

}
