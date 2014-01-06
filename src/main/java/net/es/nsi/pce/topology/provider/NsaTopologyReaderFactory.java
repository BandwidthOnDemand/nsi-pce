/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
public class NsaTopologyReaderFactory implements TopologyReaderFactory {
    @Override
    public NmlTopologyReader getReader() {
        return new NsaTopologyReader();
    }
    
    @Override
    public NmlTopologyReader getReader(String id, String target) {
        return new NsaTopologyReader(id, target);
    }
}
