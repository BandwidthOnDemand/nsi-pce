/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpType;

/**
 *
 * @author hacksaw
 */
public class StpEdge extends GraphEdge {
    private StpType stp;
    private ServiceDomainType serviceDomain;

    public StpEdge(String id, StpType stp, ServiceDomainType serviceDomain) {
        super(id);
        this.stp = stp;
        this.serviceDomain = serviceDomain;
    }

    public StpEdge(StpType stp, ServiceDomainType serviceDomain) {
        super(stp.getId());
        this.stp = stp;
        this.serviceDomain = serviceDomain;
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
}
