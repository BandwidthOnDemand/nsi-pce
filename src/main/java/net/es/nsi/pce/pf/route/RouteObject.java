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
package net.es.nsi.pce.pf.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.pf.simple.SimpleStp;
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
     * Here are the ERO rules that have been implemented:
     *   - An internal STP is considered any STP that is not discoverable in
     *     topology.  If an internal STP is specified in an ERO then it must be
     *     bounded by two external STP discoverable from topology.
     *   - An intermediate STP is specified in and ERO relative to the SRC STP.
     *     An intermediate STP is always considered an egress STP so take care
     *     when specifying to avoid weird hair pinning or non-optimal paths.
     *   - At the moment an intermediate STP must be a fully qualified STP.
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
            Optional<StpListType> ero) throws WebApplicationException {

      // Each segment of the a path is assigned a route object with an A and Z end.
      Route route = new Route();

      // Get the set of possible source STP identifiers (a range could be provided).
      StpTypeBundle srcBundle = new StpTypeBundle(topology, srcStpId, directionality);
      if (srcBundle.isEmpty()) {
          log.error("RouteObject: source STP does not exist in topology: " + srcStpId.toString());
          throw Exceptions.stpResolutionError(srcStpId.toString());
      }

      route.setBundleA(srcBundle);

      // Now we process any ERO elements if present.
      if (ero.isPresent()) {
        // We need to process the ERO in order that it was specified.
        List<OrderedStpType> orderedSTP = ero.get().getOrderedSTP();
        Collections.sort(orderedSTP, new CustomComparator());

        // Our first processed segment starts with our source STP.
        SimpleStp lastStp = srcStpId;

        for (OrderedStpType stp : orderedSTP) {
          // Parse this STP and generate a bundle enumerating all the STP.  The
          // specified STP must exist in topology for a bundle to be generated.
          SimpleStp nextStp = new SimpleStp(stp.getStp());
          StpTypeBundle nextBundle = new StpTypeBundle(topology, nextStp, directionality);

          // If we did not find an associated bundle the specified ERO STP may
          // be an internal STP used for a domain's internal path computation.
          if (nextBundle.isEmpty()) {
            // The one rule we have is that an internal STP must be bounded by
            // at least one externally visible STP from the same domain.  This
            // check is to see if the previous STP was in the same domain.
            //if (!nextStp.getNetworkId().equalsIgnoreCase(lastStp.getNetworkId())) {
                //log.error("RouteObject: Internal STP not bounded by external STP: " + stp.getStp());
                //throw Exceptions.invalidEroError(stp.getStp());
            //}

            // Save this internal STP.
            route.addInternalStp(stp.getStp());
          }
          else {
              // We have an inter-domain STP so save it and get the SDP
              // so we know the STP on far end.  We may need to filter some
              // of these out if there is no corresponding SDP (mismatching
              // labels on each end of the link).
              nextBundle = nextBundle.getPeerReducedBundle();
              if (nextBundle.isEmpty()) {
                log.error("RouteObject: ERO STP not associated with SDP: " + stp.getStp());
                throw Exceptions.invalidEroError(stp.getStp());
              }

              // We have completed this path segment by finding a external
              // interdomain STP.
              route.setBundleZ(nextBundle);
              routes.add(route);

              // Now create the bundle associated with the other end of SDP.
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

        // Add the original destination endpoint after any inserted ERO points.
        StpTypeBundle dstBundle = new StpTypeBundle(topology, dstStpId, directionality);
        if (dstBundle.isEmpty()) {
            log.error("RouteObject: destination STP does not exist in topology: " + dstStpId.toString());
            throw Exceptions.stpResolutionError(dstStpId.toString());
        }

        route.setBundleZ(dstBundle);

        // Add this last route to our list of one or more routes.
        routes.add(route);

        // We have completed building the ERO but need to check for internal
        // consistency.
        routes.forEach(r -> {
          StpTypeBundle bundleA = r.getBundleA();
          StpTypeBundle bundleZ = r.getBundleZ();
          SimpleStp stpA = bundleA.getSimpleStp();
          SimpleStp stpZ = bundleZ.getSimpleStp();

          String network = stpA.getNetworkId();
          for (OrderedStpType internalStp : r.getInternalStp()) {
            SimpleStp istp = new SimpleStp(internalStp.getStp());
            if (!istp.getNetworkId().equalsIgnoreCase(network)) {
              network = stpZ.getNetworkId();
              if (!istp.getNetworkId().equalsIgnoreCase(network)) {
                log.error("RouteObject: internal STP {} not a member of networkA {} or networkZ {}",
                        istp.getStpId(), stpA.getNetworkId(), stpZ.getNetworkId());
                throw Exceptions.invalidEroError(istp.getStpId());
              }
            }
          }
        });
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
