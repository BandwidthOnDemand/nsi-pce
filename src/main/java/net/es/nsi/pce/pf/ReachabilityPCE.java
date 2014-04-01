package net.es.nsi.pce.pf;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.cons.Constraints;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;

public class ReachabilityPCE implements PCEModule {

    @Override
    public PCEData apply(PCEData pceData) {
        Optional<Path> path = findPath(pceData);

        if (path.isPresent()) {
            pceData.setPath(path.get());
        }

        return pceData;
    }

    protected Optional<Path> findPath(PCEData pceData) {
        Constraints constraints = new Constraints(pceData.getConstraints());

        String sourceStp = getSourceStp(constraints);
        String destStp = getDestinationStp(constraints);

        String sourceTopologyId = extractTopologyIdOrFail(sourceStp, Point2Point.SOURCESTP);
        String destTopologyId = extractTopologyIdOrFail(destStp, Point2Point.DESTSTP);

        return Optional.absent();
    }


    private String getSourceStp(Constraints constraints) {
        return getStringValue(Point2Point.SOURCESTP, constraints);
    }

    private String getDestinationStp(Constraints constraints) {
        return getStringValue(Point2Point.DESTSTP, constraints);
    }

    private String getStringValue(String attributeName, Constraints constraints) {
        Optional<String> value = getValue(constraints.getStringAttrConstraint(attributeName));

        if (value.isPresent()) {
            return value.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2Point.NAMESPACE, attributeName));
    }

    private Optional<String> getValue(StringAttrConstraint constraint) {
        if (constraint == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(Strings.emptyToNull(constraint.getValue()));
    }

    private String extractTopologyIdOrFail(String stpId, String attributeName) {
        Optional<String> topologyId = extractTopologyId(stpId);
        if (topologyId.isPresent()) {
            return topologyId.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.STP_RESOLUTION_ERROR, Point2Point.NAMESPACE, attributeName));
    }

    protected Optional<String> extractTopologyId(String stpId) {
        checkNotNull(stpId);

        Iterable<String> parts = Iterables.limit(Splitter.on(":").split(stpId), 5);

        return Iterables.size(parts) == 5 ? Optional.of(Joiner.on(":").join(parts)) : Optional.<String>absent();
    }

}
