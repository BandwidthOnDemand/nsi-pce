/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.OrderedStpType;
import net.es.nsi.pce.path.jaxb.StpListType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RouteObject {
    private final Logger log = LoggerFactory.getLogger(getClass());
    List<Route> routes = new ArrayList<>();

    public RouteObject(NsiTopology topology, SimpleStp srcStpId,
            SimpleStp dstStpId, DirectionalityType directionality,
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
