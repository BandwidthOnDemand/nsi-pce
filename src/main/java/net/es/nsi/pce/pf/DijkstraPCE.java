package net.es.nsi.pce.pf;


import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.PathEndpoints;
import net.es.nsi.pce.pf.api.topo.*;

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
            networkVertex.setNetwork(net);
            networkVertex.setLocalId(netId);

            for (String stpId : net.getStpIds()) {
                Stp stp = net.getStp(stpId);
                String netEdge      = "  "+stp.getLocalId()+" -- "+networkVertex.getLocalId();
                String netEdgeInv   = "  "+networkVertex.getLocalId()+" -- "+stp.getLocalId();
                System.out.println(netEdge);
                System.out.println(netEdgeInv);

                g.addEdge(netEdge,      stp, networkVertex, EdgeType.DIRECTED);
                g.addEdge(netEdgeInv,   networkVertex, stp, EdgeType.DIRECTED);

                for (StpConnection conn: net.getConnectionsFrom(stp)) {
                    Stp remote = conn.getZ();

                    if (remote != null) {
                        String remEdge      = "  "+stp.getLocalId()+" -- "+remote.getLocalId();
                        g.addEdge(remEdge,  stp, remote, EdgeType.DIRECTED);
                        System.out.println(remEdge);
                    }

                }

            }
        }

        DijkstraShortestPath<Stp, String> alg = new DijkstraShortestPath(g);
        List<String> l = alg.getPath(srcStp, dstStp);
        System.out.println("**** PATH ****");
        System.out.println(l.toString());


        return pceData;
    }

}
