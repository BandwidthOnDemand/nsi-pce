/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.topo;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hacksaw
 */
public class Nsa {
    private String id;
    private String name;
    private double latitude = 0;
    private double longitude = 0;
    private List<String> networkIds = new ArrayList<String>();    

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
     * @return the name
     */
    public String getName() {
        if (name == null) {
            return id;
        }
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the networkIds
     */
    public List<String> getNetworkIds() {
        return networkIds;
    }

    /**
     * @param networkIds the networkIds to set
     */
    public void setNetworkIds(List<String> networkIds) {
        this.networkIds = networkIds;
    }
    
    public void addNetworkId(String networkId) {
        this.networkIds.add(networkId);
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude the latitude to set
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
