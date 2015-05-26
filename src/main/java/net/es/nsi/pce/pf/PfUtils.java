/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;

/**
 *
 * @author hacksaw
 */
public class PfUtils {
    public static String getSourceStpOrFail(AttrConstraints constraints) {
        String sourceStp = getP2PServiceBaseTypeOrFail(constraints).getSourceSTP();
        if (Strings.isNullOrEmpty(sourceStp)) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null"));
        }

        return sourceStp;
    }

    public static String getSourceStpOrFail(P2PServiceBaseType p2ps) {
        Optional<String> sourceStp = Optional.fromNullable(Strings.emptyToNull(p2ps.getSourceSTP()));
        if (sourceStp.isPresent()) {
            return sourceStp.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null"));
    }

    public static String getDestinationStpOrFail(AttrConstraints constraints) {
        String destStp = getP2PServiceBaseTypeOrFail(constraints).getDestSTP();
        if (Strings.isNullOrEmpty(destStp)) {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null"));
        }

        return destStp;
    }

    public static String getDestinationStpOrFail(P2PServiceBaseType p2ps) {
        Optional<String> destStp = Optional.fromNullable(Strings.emptyToNull(p2ps.getDestSTP()));
        if (destStp.isPresent()) {
            return destStp.get();
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null"));
    }

    public static DirectionalityType getDirectionality(P2PServiceBaseType p2ps) {
        Optional<DirectionalityType> directionality = Optional.fromNullable(p2ps.getDirectionality());
        return directionality.or(DirectionalityType.BIDIRECTIONAL);
    }

    public static boolean getSymmetricPath(P2PServiceBaseType p2ps) {
        Optional<Boolean> symmetricPath = Optional.fromNullable(p2ps.isSymmetricPath());
        return symmetricPath.or(Boolean.TRUE);
    }

    public static P2PServiceBaseType getP2PServiceBaseTypeOrFail(AttrConstraints constraints) {
        // Generic reservation information are in string constraint attributes,
        // but the P2PS specific constraints are in the P2PS P2PServiceBaseType.
        Optional<ObjectAttrConstraint> p2pObject = Optional.fromNullable(constraints.getObjectAttrConstraint(Point2PointTypes.P2PS));
        if (p2pObject.isPresent()) {
            return p2pObject.get().getValue(P2PServiceBaseType.class);
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.P2PS, "null", "null"));
    }

    public static P2PServiceBaseType removeP2PServiceBaseTypeOrFail(AttrConstraints constraints) {
        // Generic reservation information are in string constraint attributes,
        // but the P2PS specific constraints are in the P2PS P2PServiceBaseType.
        Optional<ObjectAttrConstraint> p2pObject = Optional.fromNullable(constraints.removeObjectAttrConstraint(Point2PointTypes.P2PS));
        if (p2pObject.isPresent()) {
            return p2pObject.get().getValue(P2PServiceBaseType.class);
        }

        throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.P2PS, "null", "null"));
    }
}
