/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.SimpleLabel;

/**
 *
 * @author hacksaw
 */
public class DijkstraVertex {
    private DijkstraVertexType type;
    private String id;
    private StpType stp = null;
    private SimpleLabel label = null;
    private ServiceDomainType serviceDomain = null;

    public DijkstraVertex(String id, ServiceDomainType serviceDomain) {
        this.type = DijkstraVertexType.SERVICEDOMAIN;
        this.id = id;
        this.serviceDomain = serviceDomain;
    }

    public DijkstraVertex(String id, StpType stp) {
        this.type = DijkstraVertexType.STP;
        this.id = id;
        this.stp = stp;
    }

    public DijkstraVertex(String id, StpType stp, SimpleLabel label) {
        this.type = DijkstraVertexType.STP;
        this.id = id;
        this.stp = stp;
        this.label = label;
    }

    /**
     * @return the type
     */
    public DijkstraVertexType getType() {
        return type;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the stp
     */
    public StpType getStp() {
        return stp;
    }

    /**
     * @return the label
     */
    public SimpleLabel getLabel() {
        return label;
    }

    /**
     * @return the serviceDomain
     */
    public ServiceDomainType getServiceDomain() {
        return serviceDomain;
    }
}
