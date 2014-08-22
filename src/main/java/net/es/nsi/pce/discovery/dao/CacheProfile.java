/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.dao;

/**
 *
 * @author hacksaw
 */
public class CacheProfile extends DdsProfile {

    public CacheProfile(DiscoveryConfiguration configReader) {
        super(configReader);
    }

    @Override
    public String getDirectory() {
        return getConfiguration().getCache();
    }
}
