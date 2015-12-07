/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.jaxb.topology.StpType;

/**
 *
 * @author hacksaw
 */
public class StpChooser {
    private final List<StpPair> pairs = new ArrayList<>();

    public StpChooser(StpTypeBundle a, StpTypeBundle z) {
        for (StpType stpA : a.values()) {
            for (StpType stpZ : z.values()) {
                pairs.add(new StpPair(stpA, stpZ));
            }
        }
    }

    public StpPair removeRandom() {
        int nextInt = ThreadLocalRandom.current().nextInt(pairs.size());
        return pairs.remove(nextInt);
    }

    public boolean hasNext() {
        return !pairs.isEmpty();
    }
}
