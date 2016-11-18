/*
 * NSI Path Computation Element (NSI-PCE) Copyright (c) 2013 - 2016,
 * The Regents of the University of California, through Lawrence
 * Berkeley National Laboratory (subject to receipt of any required
 * approvals from the U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.route.StpTypeBundle;
import net.es.nsi.pce.pf.simple.SimpleStp;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.topology.ResourceRefType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.ServiceDomainType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This PCE module takes a resolved path and widens the possible STP satisfying
 * the path by converting the resolved STP into under specified STP if these
 * STP can still satisfy the path request.  The goal is to provide the widest
 * possible set of feasible STPs along the viable path to give each NSA a larger
 * set of STP from which to chose a solution.  We must not violate an ERO
 * provided when performing this operation.   This module is only used for
 * sequential pathfinding requests where an NSA will create the reservation in
 * sequence from A to Z one segment at a time.
 *
 * @author hacksaw
 */
public class UnderspecifiedPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private NsiTopology nsiTopology;

    @Override
    public PCEData apply(PCEData pceData) {
        checkNotNull(pceData, "UnderspecifiedPCE: No pceData  provided");
        nsiTopology = Optional.ofNullable(pceData.getTopology()).orElseThrow(new ExceptionSupplier(Exceptions.internalServerError("UnderspecifiedPCE invoked with empty topology")));
        List<PathSegment> pathSegments = Optional.ofNullable(pceData.getPath()).orElseThrow(new ExceptionSupplier(Exceptions.noPathFound("UnderspecifiedPCE invoked with empty path results"))).getPathSegments();
        Request request = new Request(pceData);

        // If there are any STP specified in the request ERO we want to process for fast lookup.
        Map<String, SimpleStp> ero = new HashMap<>();
        if (request.getEro().isPresent()) {
            for (OrderedStpType stp : request.getEro().get().getOrderedSTP()) {
                SimpleStp sstp = new SimpleStp(stp.getStp());
                ero.put(sstp.getId(), sstp);
            }
        }

        for (PathSegment pathSegment : pathSegments){
            log.debug("UnderspecifiedPCE: stpA=" + pathSegment.getA() + ", stpZ=" + pathSegment.getZ());

            ObjectAttrConstraint objectConstraint = pathSegment.getConstraints().getObjectAttrConstraint(Point2PointTypes.P2PS);
            P2PServiceBaseType p2ps = (P2PServiceBaseType) objectConstraint.getValue();

            // Attempt to widen STP A if not originating or terminating STP (these
            // have already been widened).
            if (!request.getSourceStp().equalsIgnoreCase(pathSegment.getA()) &&
                    !request.getDestStp().equalsIgnoreCase(pathSegment.getA())) {
                // We can try to expand the A end of this segment.
                log.debug("UnderspecifiedPCE: original A " + pathSegment.getA());
                SimpleStp result = expand(pathSegment.getA(), ero);
                log.debug("UnderspecifiedPCE: modified A " + result.getStpId());
                pathSegment.setA(result.getStpId());
                p2ps.setSourceSTP(pathSegment.getA());
            }

            // Attempt to widen STP Z if not originating or terminating STP (these
            // have already been widened).
            if (!request.getSourceStp().equalsIgnoreCase(pathSegment.getZ()) &&
                    !request.getDestStp().equalsIgnoreCase(pathSegment.getZ())) {
                // We can try to expand the Z end of this segment.
                log.debug("UnderspecifiedPCE: original Z " + pathSegment.getZ());
                SimpleStp result = expand(pathSegment.getZ(), ero);
                log.debug("UnderspecifiedPCE: modified Z " + result.getStpId());
                pathSegment.setZ(result.getStpId());
                p2ps.setDestSTP(pathSegment.getZ());
            }
        }

        pceData.setPath(new Path(pathSegments));

        // Build a path response for each segment in the the computed path.
        return pceData;
    }

    /**
     * Return the widest possible under specified STP based on the range of
     * available SDP on this port while not violating any switching service
     * or ERO restrictions.
     *
     * @param stpId
     * @return
     */
    private SimpleStp expand(String stpId, Map<String, SimpleStp> ero) {
        StpType stp = Optional.ofNullable(nsiTopology.getStp(stpId)).orElseThrow(new ExceptionSupplier(Exceptions.internalServerError("UnderspecifiedPCE unknown stpId " + stpId)));
        ResourceRefType serviceDomainRef = Optional.ofNullable(stp.getServiceDomain()).orElseThrow(new ExceptionSupplier(Exceptions.internalServerError("UnderspecifiedPCE no service domain for " + stpId)));
        ServiceDomainType localServiceDomain = nsiTopology.getServiceDomain(serviceDomainRef.getId());
        SimpleStp localStp = new SimpleStp(stpId);

        // We need to be a member of a ServiceDomain to have a chance to be
        // expanded.
        if (null == localServiceDomain) {
            log.error("expand: no service domain for the stpid = " + stpId);
            return localStp;
        }

        // If labelSwapping is not supported in the local ServiceDomain then
        // we cannot expand.
        if (!localServiceDomain.isLabelSwapping()) {
            log.debug("expand: local ServiceDomain does not support labelSwapping, id = " + localServiceDomain.getId());
            return localStp;
        }

        // If this STP is a member of an SDP then we try to expand the list of
        // applicable STP.
        Set<SdpType> sdpMember = nsiTopology.getSdpMember(stpId);
        if (1 != sdpMember.size()) {
            // This STP is not a member of any SDP so it must be a source or
            // destination STP.
            log.error("expand: STP not a member of an SDP, stpid = " + stpId);
            return localStp;
        }

        // Get the ServiceDomain on the remote end of the SDP.
        SdpType sdp = sdpMember.iterator().next();
        ServiceDomainType remoteServiceDomain;
        SimpleStp remoteStp;
        if (stpId.equalsIgnoreCase(sdp.getDemarcationA().getStp().getId())) {
            remoteServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationZ().getServiceDomain().getId());
            remoteStp = new SimpleStp(sdp.getDemarcationZ().getStp().getId());
        }
        else {
            remoteServiceDomain = nsiTopology.getServiceDomain(sdp.getDemarcationA().getServiceDomain().getId());
            remoteStp = new SimpleStp(sdp.getDemarcationA().getStp().getId());
        }

        // If labelSwapping is not supported in the remote ServiceDomain then
        // we cannot expand.
        if (!remoteServiceDomain.isLabelSwapping()) {
            log.debug("expand: remote ServiceDomain does not support labelSwapping, id = " + remoteServiceDomain.getId());
            return localStp;
        }

        // So now we know both the local and remote ServiceDomain support
        // label swapping so it is possible to expand the STP.
        StpTypeBundle localStpBundle = nsiTopology.getStpTypeBundle(localStp.getId());
        StpTypeBundle remoteStpBundle = nsiTopology.getStpTypeBundle(remoteStp.getId());

        // We should do more dilligence and validate the local STP range use
        // SDP sharing a remote serviceDomain but this takes a few looksups
        // each taking too much time.  Instead we are going to cheat and
        // assume all STP in the bundle are connected to the same remote STP
        // bundle, and that they all share the same remote ServiceDomain.
        SimpleStp resultStp = new SimpleStp(localStpBundle.getSimpleStp().toString());
        resultStp.intersectLabels(remoteStpBundle.getSimpleStp().getLabels());

        // Make sure not to violate any specified ERO.
        SimpleStp localEro = ero.get(localStpBundle.getSimpleStp().getId());
        if (localEro != null) {
            log.debug("expand: stpId (" + resultStp.toString() + ") is referenced in ERO (" + localEro.toString() + ").");
            if (!localEro.getLabels().isEmpty()) {
                resultStp.intersectLabels(localEro.getLabels());
            }
        }

        SimpleStp remoteEro = ero.get(remoteStpBundle.getSimpleStp().getId());
        if (remoteEro != null) {
            log.debug("expand: stpId (" + resultStp.toString() + ") is referenced in ERO (" + remoteEro.toString() + ").");
            if (!remoteEro.getLabels().isEmpty()) {
                resultStp.intersectLabels(remoteEro.getLabels());
            }
        }

        return resultStp;
    }
}