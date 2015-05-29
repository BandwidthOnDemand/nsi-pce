package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;

/**
 *
 * @author hacksaw
 */
public class StpTypeBundle {
    private final Map<String, StpType> bundle = new HashMap<>();
    private final SimpleStp stp;

    public StpTypeBundle(NsiTopology topology, SimpleStp stp, DirectionalityType directionality) {
        this.stp = stp;

        for (String stpId : stp.getMemberStpId()) {
            Optional<StpType> stpType = Optional.fromNullable(topology.getStp(stpId));
            if (stpType.isPresent()) {
                validateDirectionality(stpType.get(), directionality);
                bundle.put(stpId, stpType.get());
            }
        }
    }

    public SimpleStp getSimpleStp() {
        return stp;
    }

    public Map<String, StpType> getStpBundle() {
        return Collections.unmodifiableMap(bundle);
    }

    public Collection<StpType> values() {
        return bundle.values();
    }

    public Set<String> keySet() {
        return bundle.keySet();
    }

    private void validateDirectionality(StpType stp, DirectionalityType directionality) throws IllegalArgumentException {
        // Verify the specified STP are of the correct type for the request.
        if (directionality == DirectionalityType.UNIDIRECTIONAL) {
             if (stp.getType() != StpDirectionalityType.INBOUND &&
                     stp.getType() != StpDirectionalityType.OUTBOUND) {
                throw Exceptions.bidirectionalStpInUnidirectionalRequest(stp.getId());
            }
        }
        else {
            if (stp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw Exceptions.unidirectionalStpInBidirectionalRequest(stp.getId());
            }
        }
    }

    public int size() {
        return bundle.size();
    }

    public boolean isEmpty() {
        return bundle.isEmpty();
    }

    public boolean contains(Object o) {
        return bundle.containsValue((StpType) o);
    }

    public Iterator iterator() {
        return bundle.values().iterator();
    }

    public Object[] toArray() {
        return bundle.values().toArray();
    }
}
