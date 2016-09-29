package net.es.nsi.pce.pf;

import java.util.List;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.StpPair;

/**
 *
 * @author hacksaw
 */
public interface PathResponse {
    public PCEData build(PCEData pceData, Request request, List<StpPair> segments);
}
