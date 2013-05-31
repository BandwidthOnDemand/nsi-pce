package net.es.nsi.pce.pf.api;


import net.es.nsi.pce.pf.api.topo.Network;

import java.util.ArrayList;
import java.util.List;

public class Path {
    private List<StpPair> stpPairs = new ArrayList<StpPair>();

    public List<StpPair> getStpPairs() {
        return stpPairs;
    }

}
