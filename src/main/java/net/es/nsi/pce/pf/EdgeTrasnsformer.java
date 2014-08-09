package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public final class EdgeTrasnsformer implements Transformer<GraphEdge, Number> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private PCEData pceData;

    public EdgeTrasnsformer(PCEData pceData) {
        this.pceData = pceData;
    }

    @Override
    public Number transform(GraphEdge e) {
        return 1;
    }

}
