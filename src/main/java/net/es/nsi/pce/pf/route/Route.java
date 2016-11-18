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
import java.util.List;
import net.es.nsi.pce.jaxb.path.OrderedStpType;

/**
 * Model a single network connection segment where A and Z bundles represent
 * under specified edge STP bounding any internal STP specified in an ERO.
 *
 * @author hacksaw
 */
public class Route {
    private final net.es.nsi.pce.jaxb.path.ObjectFactory factory = new net.es.nsi.pce.jaxb.path.ObjectFactory();

    private StpTypeBundle bundleA;
    private StpTypeBundle bundleZ;
    private final List<OrderedStpType> internalStp = new ArrayList<>();
    private int order = 0;

    /**
     * @return the bundleA
     */
    public StpTypeBundle getBundleA() {
        return bundleA;
    }

    /**
     * @param bundleA the bundleA to set
     */
    public void setBundleA(StpTypeBundle bundleA) {
        this.bundleA = bundleA;
    }

    /**
     * @return the bundleZ
     */
    public StpTypeBundle getBundleZ() {
        return bundleZ;
    }

    /**
     * @param bundleZ the bundleZ to set
     */
    public void setBundleZ(StpTypeBundle bundleZ) {
        this.bundleZ = bundleZ;
    }

    public void addInternalStp(String stpId) {
        OrderedStpType stp = factory.createOrderedStpType();
        stp.setOrder(order++);
        stp.setStp(stpId);
        this.internalStp.add(stp);
    }

    public List<OrderedStpType> getInternalStp() {
        return Collections.unmodifiableList(internalStp);
    }

    @Override
    public boolean equals(Object object){
        if (object == this) {
            return true;
        }

        if((object == null) || (object.getClass() != this.getClass())) {
            return false;
        }

        Route that = (Route) object;
        if (!this.bundleA.equals(that.getBundleA())) {
            return false;
        }

        if (!this.bundleZ.equals(that.getBundleZ())) {
            return false;
        }

        if (this.internalStp.size() != that.getInternalStp().size()) {
            return false;
        }

        for (OrderedStpType stp : this.internalStp) {
            boolean found = false;
            for (OrderedStpType thatStp : that.getInternalStp()) {
                if (stp.getOrder() == thatStp.getOrder() &&
                        stp.getStp().equalsIgnoreCase(thatStp.getStp())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((bundleA == null) ? 0 : bundleA.hashCode());
        result = prime * result
                + ((bundleZ == null) ? 0 : bundleZ.hashCode());
        result = prime * result
                + ((internalStp == null) ? 0 : internalStp.hashCode());
        return result;
    }
}
