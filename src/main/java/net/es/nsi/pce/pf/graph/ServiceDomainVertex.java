/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.graph;

import net.es.nsi.pce.pf.graph.GraphVertex;
import net.es.nsi.pce.jaxb.topology.ServiceDomainType;

/**
 *
 * @author hacksaw
 */
public class ServiceDomainVertex extends GraphVertex {
    private ServiceDomainType serviceDomain = null;

    public ServiceDomainVertex(String id, ServiceDomainType serviceDomain) {
        super(id);
        this.serviceDomain = serviceDomain;
    }

    /**
     * @return the serviceDomain
     */
    public ServiceDomainType getServiceDomain() {
        return serviceDomain;
    }
}
