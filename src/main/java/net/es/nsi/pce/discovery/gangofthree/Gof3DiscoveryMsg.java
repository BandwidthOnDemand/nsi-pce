/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.gangofthree;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author hacksaw
 */
public class Gof3DiscoveryMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nsaURL;
    // private String topologyURL;
    private long nsaLastModifiedTime = 0;
    // private long topologyLastModifiedTime = 0;
    private String nsaId;
    private Map<String, Long> topology = new HashMap<>(); // key = topologyURL, Long == topologyLastModifiedTime

    /**
     * @return the nsaURL
     */
    public String getNsaURL() {
        return nsaURL;
    }

    /**
     * @param nsaURL the nsaURL to set
     */
    public void setNsaURL(String nsaURL) {
        this.nsaURL = nsaURL;
    }

    public void addTopology(String topologyURL, Long topologyLastModifiedTime) {
        this.topology.put(topologyURL, topologyLastModifiedTime);
    }

    public Set<String> getTopologyURL() {
        return this.topology.keySet();
    }

    public Long removeTopologyURL(String url) {
        return this.topology.remove(url);
    }

    public Map<String, Long> getTopology() {
        return Collections.unmodifiableMap(this.topology);
    }

    public Long getTopologyLastModified(String url) {
        return topology.get(url);
    }

    public Long setTopologyLastModified(String url, Long lastModifiedTime) {
        return topology.put(url, lastModifiedTime);
    }

    /**
     * @return the nsaLastModifiedTime
     */
    public long getNsaLastModifiedTime() {
        return nsaLastModifiedTime;
    }

    /**
     * @param nsaLastModifiedTime the nsaLastModifiedTime to set
     */
    public void setNsaLastModifiedTime(long nsaLastModifiedTime) {
        this.nsaLastModifiedTime = nsaLastModifiedTime;
    }

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }
}
