package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.PathEndpoints;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Stp;

public class PretendPCE implements PCEModule {
    public PCEData apply(PCEData pceData) throws Exception {
        PathEndpoints pe = null;
        for (Constraint c : pceData.getConstraints()) {
            if (c instanceof PathEndpoints) {
                pe = (PathEndpoints) c;
            }
        }
        Path path = new Path();



        if (pe.getSrcNetwork().equals(pe.getDstNetwork())) {
            Network srcNet = new Network();
            srcNet.setNetworkId(pe.getSrcNetwork());
            Stp a = new Stp();
            a.setLocalId(pe.getSrcLocal());
            a.setNetwork(srcNet);
            Stp z = new Stp();
            z.setLocalId(pe.getDstLocal());
            z.setNetwork(srcNet);
            StpPair stpPair = new StpPair();
            stpPair.setA(a);
            stpPair.setZ(z);
            path.getStpPairs().add(stpPair);


        } else {
            Network srcNet = new Network();
            srcNet.setNetworkId(pe.getSrcNetwork());
            Network dstNet = new Network();
            dstNet.setNetworkId(pe.getDstNetwork());
            Stp a = new Stp();
            a.setLocalId(pe.getSrcLocal());
            a.setNetwork(srcNet);

            Stp b = new Stp();
            b.setLocalId("pretend-B");
            b.setNetwork(srcNet);


            Stp y = new Stp();
            y.setLocalId("pretend-Y");
            y.setNetwork(dstNet);

            Stp z = new Stp();
            z.setLocalId(pe.getDstLocal());
            z.setNetwork(dstNet);

            StpPair first = new StpPair();
            first.setA(a);
            first.setZ(b);
            StpPair second = new StpPair();
            second.setA(a);
            second.setZ(b);
            path.getStpPairs().add(first);
            path.getStpPairs().add(second);
        }



        PCEData result = new PCEData();
        result.setPath(path);
        return result;

    }
}
