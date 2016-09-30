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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RouteObject {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final net.es.nsi.pce.jaxb.path.ObjectFactory factory = new net.es.nsi.pce.jaxb.path.ObjectFactory();
    List<Route> routes = new ArrayList<>();

    /**
     * Segment the path request into individual path segments that will need
     * to be satisfied based on supplied ERO information.
     *
     * @param topology The NSI network topology used to create route object.
     * @param srcStpId The source STP identifier for the route.
     * @param dstStpId The destination STP identifier for the route.
     * @param directionality The directionality of the route (uni or bi).
     * @param ero The intermediate points that must be in the resulting path.
     */
    public RouteObject(
            NsiTopology topology,
            SimpleStp srcStpId,
            SimpleStp dstStpId,
            DirectionalityType directionality,
            Optional<StpListType> ero) {

        Route route = new Route();

        // Get the set of possible source and destination ports.
        StpTypeBundle srcBundle = new StpTypeBundle(topology, srcStpId, directionality);
        if (srcBundle.isEmpty()) {
            log.error("RouteObject: source STP does not exist in topology: " + srcStpId.toString());
            throw Exceptions.stpResolutionError(srcStpId.toString());
        }

        route.setBundleA(srcBundle);

        if (ero.isPresent()) {
            List<OrderedStpType> orderedSTP = ero.get().getOrderedSTP();
            Collections.sort(orderedSTP, new CustomComparator());
            SimpleStp lastStp = srcStpId;
            for (OrderedStpType stp : orderedSTP) {
                log.debug("RouteObject: order=" + stp.getOrder() + ", stpId=" + stp.getStp());
                SimpleStp nextStp = new SimpleStp(stp.getStp());
                StpTypeBundle nextBundle = new StpTypeBundle(topology, nextStp, directionality);

                if (nextBundle.isEmpty()) {
                    if (!nextStp.getNetworkId().equalsIgnoreCase(lastStp.getNetworkId())) {
                        log.error("RouteObject: Internal STP not bounded by external: " + stp.getStp());
                        //throw Exceptions.stpResolutionError(stp.getStp());
                    }

                    // Must be an internal STP.
                    route.addInternalStp(stp.getStp());
                }
                else {
                    // We have an inter-domain STP so save it and get SDP.
                    route.setBundleZ(nextBundle);
                    routes.add(route);
                    route = new Route();

                    StpTypeBundle lastBundle = new StpTypeBundle();
                    for (StpType member : nextBundle.values()) {
                        SdpType sdp = topology.getSdp(member.getSdp().getId());
                        if (sdp == null) {
                            log.error("RouteObject: ERO STP not associated with valid SDP in context of request: " + stp.getStp());
                            throw Exceptions.invalidEroMember(stp.getStp());
                        }
                        if (member.getId().equalsIgnoreCase(sdp.getDemarcationA().getStp().getId())) {
                            lastBundle.addStpType(topology.getStp(sdp.getDemarcationZ().getStp().getId()));
                        }
                        else {
                            lastBundle.addStpType(topology.getStp(sdp.getDemarcationA().getStp().getId()));
                        }
                    }

                    if (lastBundle.isEmpty()) {
                        log.error("RouteObject: ERO STP not associated with SDP: " + stp.getStp());
                        throw Exceptions.invalidEroError(stp.getStp());
                    }

                    route.setBundleA(lastBundle);
                    lastStp = lastBundle.getSimpleStp();
                }
            }
        }

        StpTypeBundle dstBundle = new StpTypeBundle(topology, dstStpId, directionality);
        if (dstBundle.isEmpty()) {
            log.error("RouteObject: destination STP does not exist in topology: " + dstStpId.toString());
            throw Exceptions.stpResolutionError(dstStpId.toString());
        }

        route.setBundleZ(dstBundle);
        routes.add(route);
    }

    public List<Route> getRoutes() {
        return routes;
    }

    // make this network for internal ERO and not STP
    public StpListType getInternalERO(String networkId) {
        StpListType list = factory.createStpListType();
        for (Route route : this.getRoutes()) {
            for (OrderedStpType internal : route.getInternalStp()) {
                if (networkId.equalsIgnoreCase(SimpleStp.parseNetworkId(internal.getStp()))) {
                    list.getOrderedSTP().add(internal);
                }
            }

            if (!list.getOrderedSTP().isEmpty()) {
                return list;
            }
        }

        if (!list.getOrderedSTP().isEmpty()) {
            return list;
        }

        return null;
    }

    public int size() {
        return routes.size();
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    public boolean add(Route e) {
        return routes.add(e);
    }

    public boolean remove(Route o) {
        return routes.remove(o);
    }

    public void clear() {
        routes.clear();
    }

    public class CustomComparator implements Comparator<OrderedStpType> {
        @Override
        public int compare(OrderedStpType o1, OrderedStpType o2) {
            if (o1.getOrder() > o2.getOrder()) {
                return 1;
            }
            else if (o1.getOrder() < o2.getOrder()) {
                return -1;
            }

            return 0;
        }
    }
}
