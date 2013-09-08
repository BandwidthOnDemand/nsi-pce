package net.es.nsi.pce.pf;


import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;
import net.es.nsi.pce.pf.api.topo.*;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author hacksaw
 */
public class DijkstraPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public PCEData apply(PCEData pceData) throws Exception {
        TopoPathEndpoints pe = null;
        for (Constraint c : pceData.getConstraints()) {
            if (c instanceof TopoPathEndpoints) {
                pe = (TopoPathEndpoints) c;
            }
        }

        if (pe == null) {
            throw new Exception("DijkstraPCE.apply: No path endpoints found.");
        }
                
        Topology topo = pceData.getTopology();

        Network srcNet = topo.getNetwork(pe.getSrcNetwork());
        Network dstNet = topo.getNetwork(pe.getDstNetwork());
        
        if (srcNet == null) {
            throw new Exception("DijkstraPCE.apply: Unknown src network " + pe.getSrcNetwork());
        }
        else if (dstNet == null) {
            throw new Exception("DijkstraPCE.apply: Unknown dst network " + pe.getDstNetwork());
        }
        
        Stp srcStp = srcNet.getStp(pe.getSrcLocal());
        Stp dstStp = dstNet.getStp(pe.getDstLocal());

        if (srcStp == null) {
            throw new Exception("DijkstraPCE.apply: Unknown src stp " + pe.getSrcLocal());
        }
        else if (dstStp == null) {
            throw new Exception("DijkstraPCE.apply: Unknown dst stp " + pe.getDstLocal());
        }
        
        log.debug("DijkstraPCE.apply: src stp:" + srcStp);
        log.debug("DijkstraPCE.apply: dst stp:" + dstStp);

        DirectedSparseMultigraph<String, String> g = new DirectedSparseMultigraph<String, String>();
        HashMap<String, StpPair> pairMap = new HashMap<>();

        for (String netId : topo.getNetworkIds()) {
            log.debug("DijkstraPCE.apply: Edges for network " + netId);

            Network net = topo.getNetwork(netId);

            for (String stpId : net.getStpIds()) {

                Stp stp = net.getStp(stpId);
                if (stp == null) {
                    throw new Exception("DijkstraPCE.apply: No stp found for " + stpId);
                }
                g.addVertex(stp.toString());

                for (String otherStpId : net.getStpIds()) {

                    if (stpId.equals(otherStpId)) {
                        continue;
                    }

                    Stp otherStp = net.getStp(otherStpId);
                    if (otherStp == null) {
                        throw new Exception("no stp found for "+otherStpId);
                    }
                    g.addVertex(otherStp.toString());

                    StpPair az = new StpPair();
                    az.setA(stp);
                    az.setZ(otherStp);
                    String azEdge = az.toString();
                    pairMap.put(azEdge, az);

                    StpPair za = new StpPair();
                    za.setA(otherStp);
                    za.setZ(stp);
                    String zaEdge = za.toString();
                    pairMap.put(zaEdge, za);


                    g.addEdge(azEdge,   stp.toString(), otherStp.toString(), EdgeType.DIRECTED);
                    g.addEdge(zaEdge,   otherStp.toString(), stp.toString(), EdgeType.DIRECTED);

                    // System.out.println("edge in net: "+stp.toString()+" "+otherStp.toString());
                }

                for (StpConnection conn: net.getConnectionsFrom(stp)) {
                    StpPair remPair = new StpPair();
                    Stp remote = conn.getZ();
                    if (remote != null) {
                        remPair.setA(stp);
                        remPair.setZ(remote);

                        String edge = remPair.toString();
                        pairMap.put(edge, remPair);

                        // System.out.println("remote edge: "+stp.toString()+ " "+remote.toString());

                        g.addEdge(edge, stp.toString(), remote.toString(), EdgeType.DIRECTED);
                    }

                }

            }
        }

        if (!g.containsVertex(srcStp.toString())) {
            throw new Exception("source stp is not contained in topology");
        } else if (!g.containsVertex(dstStp.toString())) {
            throw new Exception("dest stp is not contained in topology");
        }

        List<String> path;
        DijkstraShortestPath<String, String> alg = new DijkstraShortestPath(g);
        try {
            path = alg.getPath(srcStp.toString(), dstStp.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }

        int i = 0;
        for (String edge: path) {
            StpPair p = pairMap.get(edge);
            if (p.getA().getNetwork().getNetworkId().equals(p.getZ().getNetwork().getNetworkId())) {
                //System.out.println("path edge "+edge);
                StpPair pathPair = new StpPair();
                pathPair.setA(p.getA());
                pathPair.setZ(p.getZ());
                pceData.getPath().getStpPairs().add(i, pathPair);
                i++;
            } else {
                // System.err.println("not adding edge "+edge);
            }

        }
        /*
        System.out.println("**** PATH **** (length "+pceData.getPath().getStpPairs().size()+")");
        for (StpPair pair : pceData.getPath().getStpPairs()) {
            System.out.println(pair.getA().toString()+ " -- "+ pair.getZ().toString());
        }
        */



        return pceData;
    }

}
