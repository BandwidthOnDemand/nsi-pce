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

import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.ServiceDomainType;
import net.es.nsi.pce.jaxb.topology.StpType;

/**
 *
 * @author hacksaw
 */
public class DijkstraEdge {
    private DijkstraEdgeType type;
    private String id;
    private StpType stp;
    private SimpleLabel label = null;
    private ServiceDomainType serviceDomain;
    private SdpType sdp;

    public DijkstraEdge(String id, StpType stp, SimpleLabel label, ServiceDomainType serviceDomain) {
        this.type = DijkstraEdgeType.STP_SERVICEDOMAIN;
        this.id = id;
        this.stp = stp;
        this.label = label;
        this.serviceDomain = serviceDomain;
    }

    public DijkstraEdge(StpType stp, ServiceDomainType serviceDomain) {
        this.type = DijkstraEdgeType.STP_SERVICEDOMAIN;
        this.stp = stp;
        this.serviceDomain = serviceDomain;
        this.id = stp.getId();
    }

    public DijkstraEdge(SdpType sdp) {
        this.type = DijkstraEdgeType.SDP;
        this.id = sdp.getId();
        this.sdp = sdp;
    }

    public DijkstraEdge(String id, SdpType sdp, SimpleLabel label) {
        this.type = DijkstraEdgeType.SDP;
        this.id = id;
        this.sdp = sdp;
        this.label = label;
    }

    /**
     * @return the type
     */
    public DijkstraEdgeType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(DijkstraEdgeType type) {
        this.type = type;
    }

    /**
     * @return the stp
     */
    public StpType getStp() {
        return stp;
    }

    /**
     * @param stp the stp to set
     */
    public void setStp(StpType stp) {
        this.stp = stp;
    }

    /**
     * @return the serviceDomain
     */
    public ServiceDomainType getServiceDomain() {
        return serviceDomain;
    }

    /**
     * @param serviceDomain the serviceDomain to set
     */
    public void setServiceDomain(ServiceDomainType serviceDomain) {
        this.serviceDomain = serviceDomain;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the sdp
     */
    public SdpType getSdp() {
        return sdp;
    }

    /**
     * @param sdp the sdp to set
     */
    public void setSdp(SdpType sdp) {
        this.sdp = sdp;
    }

    /**
     * @return the label
     */
    public SimpleLabel getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(SimpleLabel label) {
        this.label = label;
    }
}
