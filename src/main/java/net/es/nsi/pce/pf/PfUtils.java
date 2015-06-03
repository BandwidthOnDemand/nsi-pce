/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.jaxb.StpListType;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class PfUtils {
    private final static Logger log = LoggerFactory.getLogger(PfUtils.class);

    public static String getServiceTypeOrFail(AttrConstraints constraints) {
        return getStringValue(PCEConstraints.SERVICETYPE, constraints);
    }

    public static String getSourceStpOrFail(AttrConstraints constraints) {
        String sourceStp = getP2PServiceBaseTypeOrFail(constraints).getSourceSTP();
        if (Strings.isNullOrEmpty(sourceStp)) {
            throw Exceptions.missingParameter(Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null");
        }

        return sourceStp;
    }

    public static String getSourceStpOrFail(P2PServiceBaseType p2ps) {
        Optional<String> sourceStp = Optional.fromNullable(Strings.emptyToNull(p2ps.getSourceSTP()));
        if (sourceStp.isPresent()) {
            return sourceStp.get();
        }

        throw Exceptions.missingParameter(Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null");
    }

    public static String getDestinationStpOrFail(AttrConstraints constraints) {
        String destStp = getP2PServiceBaseTypeOrFail(constraints).getDestSTP();
        if (Strings.isNullOrEmpty(destStp)) {
            throw Exceptions.missingParameter(Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null");
        }

        return destStp;
    }

    public static String getDestinationStpOrFail(P2PServiceBaseType p2ps) {
        Optional<String> destStp = Optional.fromNullable(Strings.emptyToNull(p2ps.getDestSTP()));
        if (destStp.isPresent()) {
            return destStp.get();
        }

        throw Exceptions.missingParameter(Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null");
    }

    public static DirectionalityType getDirectionality(P2PServiceBaseType p2ps) {
        Optional<DirectionalityType> directionality = Optional.fromNullable(p2ps.getDirectionality());
        return directionality.or(DirectionalityType.BIDIRECTIONAL);
    }

    public static boolean getSymmetricPath(P2PServiceBaseType p2ps) {
        Optional<Boolean> symmetricPath = Optional.fromNullable(p2ps.isSymmetricPath());
        return symmetricPath.or(Boolean.TRUE);
    }

    public static Optional<StpListType> getEro(P2PServiceBaseType p2ps) {
        Optional<StpListType> ero = Optional.fromNullable(p2ps.getEro());
        return ero;
    }

    public static P2PServiceBaseType getP2PServiceBaseTypeOrFail(AttrConstraints constraints) {
        // Generic reservation information are in string constraint attributes,
        // but the P2PS specific constraints are in the P2PS P2PServiceBaseType.
        Optional<ObjectAttrConstraint> p2pObject = Optional.fromNullable(constraints.getObjectAttrConstraint(Point2PointTypes.P2PS));
        if (p2pObject.isPresent()) {
            return p2pObject.get().getValue(P2PServiceBaseType.class);
        }

        throw Exceptions.missingParameter(Point2PointTypes.P2PS, "null", "null");
    }

    public static P2PServiceBaseType removeP2PServiceBaseTypeOrFail(AttrConstraints constraints) {
        // Generic reservation information are in string constraint attributes,
        // but the P2PS specific constraints are in the P2PS P2PServiceBaseType.
        Optional<ObjectAttrConstraint> p2pObject = Optional.fromNullable(constraints.removeObjectAttrConstraint(Point2PointTypes.P2PS));
        if (p2pObject.isPresent()) {
            return p2pObject.get().getValue(P2PServiceBaseType.class);
        }

        throw Exceptions.missingParameter(Point2PointTypes.P2PS, "null", "null");
    }

    public static SimpleStp getSimpleStpOrFail(String stpId) {
        // Parse the STP to make sure it is valid.
        SimpleStp simple;
        try {
            simple = new SimpleStp(stpId);
        }
        catch (IllegalArgumentException ex) {
            log.error("getSimpleStpOrFail: stpId=" + stpId, ex);
            throw Exceptions.stpResolutionError(stpId);
        }

        return simple;
    }


    public static ServiceDomainType getServiceDomainOrFail(NsiTopology topology, StpType stp) {
        Optional<ResourceRefType> serviceDomain = Optional.fromNullable(stp.getServiceDomain());
        if (serviceDomain.isPresent()) {
            Optional<ServiceDomainType> sd = Optional.fromNullable(topology.getServiceDomain(stp.getServiceDomain().getId()));
            if (sd.isPresent()) {
                return sd.get();
            }
        }
        throw Exceptions.noPathFound("Missing ServiceDomain for source sdpId=" + stp.getId());
    }

    private static String getStringValue(String attributeName, AttrConstraints constraints) {
        Optional<String> value = getValue(constraints.getStringAttrConstraint(attributeName));

        if (value.isPresent()) {
            return value.get();
        }

        throw Exceptions.missingParameter(Point2PointTypes.P2PS, attributeName, null);
    }

    private static Optional<String> getValue(StringAttrConstraint constraint) {
        if (constraint == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(Strings.emptyToNull(constraint.getValue()));
    }
}
