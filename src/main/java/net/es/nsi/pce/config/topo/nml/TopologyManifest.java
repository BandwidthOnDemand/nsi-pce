/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo.nml;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Models the master topology list as converted from NML.
 * 
 * @author hacksaw
 */
public class TopologyManifest {
    private String id;
    private long version;
    private Map<String, String> entryList = new ConcurrentHashMap<String, String>();

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
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * @return the entryList
     */
    public Map<String, String> getEntryList() {
        return Collections.unmodifiableMap(entryList);
    }

    /**
     * @param entryList the entryList to set
     */
    public void setEntryList(Map<String, String> entryList) {
        this.entryList = entryList;
    }
    
    /**
     * @return the String
     */
    public String getTopologyURL(String id) {
        return entryList.get(id);
    }

    public void setTopologyURL(String id, String url) {
        entryList.put(id, url);
    }
    
    public void put(String id, String url) {
        entryList.put(id, url);
    }    
}
