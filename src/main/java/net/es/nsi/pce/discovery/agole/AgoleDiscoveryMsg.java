/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.agole;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
public class AgoleDiscoveryMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nsaURL;
    private String topologyURL;
    private long nsaLastModifiedTime = 0;
    private long topologyLastModifiedTime = 0;
    private String nsaId;

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

    /**
     * @return the topologyURL
     */
    public String getTopologyURL() {
        return topologyURL;
    }

    /**
     * @param topologyURL the topologyURL to set
     */
    public void setTopologyURL(String topologyURL) {
        this.topologyURL = topologyURL;
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
     * @return the topologyLastModifiedTime
     */
    public long getTopologyLastModifiedTime() {
        return topologyLastModifiedTime;
    }

    /**
     * @param topologyLastModifiedTime the topologyLastModifiedTime to set
     */
    public void setTopologyLastModifiedTime(long topologyLastModifiedTime) {
        this.topologyLastModifiedTime = topologyLastModifiedTime;
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
