/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.visualization;

import net.es.nsi.pce.jaxb.topology.NetworkType;

/**
 *
 * @author hacksaw
 */
public class NetworkVertex extends NetworkType {

    public NetworkVertex() {}

    public NetworkVertex(NetworkType network) {
        this.id = network.getId();
        this.href = network.getHref();
        this.name = network.getName();
        this.nsa = network.getNsa();
        this.service = network.getService();
        this.stp = network.getStp();
        this.serviceDomain = network.getServiceDomain();
        this.any = network.getAny();
        this.version = network.getVersion();
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
