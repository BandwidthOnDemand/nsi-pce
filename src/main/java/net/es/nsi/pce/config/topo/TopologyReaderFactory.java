/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

/**
 *
 * @author hacksaw
 */
public interface TopologyReaderFactory {
    public NmlTopologyReader getReader();
    public NmlTopologyReader getReader(String target);
}
