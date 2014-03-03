package net.es.nsi.pce.pf.api;

import java.util.ArrayList;
import java.util.List;

public class Path {
    private List<StpPair> stpPairs = new ArrayList<>();

    public List<StpPair> getStpPairs() {
        return stpPairs;
    }
}
