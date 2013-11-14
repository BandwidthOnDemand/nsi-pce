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
public class FileTopologyReaderFactory implements TopologyReaderFactory {
    @Override
    public NmlTopologyReader getReader() {
        return new FileTopologyReader();
    }
    
    @Override
    public NmlTopologyReader getReader(String target) {
        return new FileTopologyReader(target);
    }
}
