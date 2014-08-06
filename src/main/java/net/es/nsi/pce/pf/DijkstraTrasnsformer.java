package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public final class DijkstraTrasnsformer implements Transformer<DijkstraEdge, Number> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private PCEData pceData;

    public DijkstraTrasnsformer(PCEData pceData) {
        this.pceData = pceData;
    }

    @Override
    public Number transform(DijkstraEdge e) {
        return 1;
    }

}