/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api;

import java.io.Serializable;
import net.es.nsi.pce.pf.api.cons.Constraints;

/**
 *
 * @author hacksaw
 */
public class PathSegment implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String nsaId;
    private String csProviderURL;
    private StpPair stpPair;
    private Constraints constraints = new Constraints();

    public PathSegment() {
    }
    
    public PathSegment(StpPair stpPair) {
        this.stpPair = stpPair;
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

    /**
     * @return the csProviderURL
     */
    public String getCsProviderURL() {
        return csProviderURL;
    }

    /**
     * @param csProviderURL the csProviderURL to set
     */
    public void setCsProviderURL(String csProviderURL) {
        this.csProviderURL = csProviderURL;
    }

    /**
     * @return the stpPair
     */
    public StpPair getStpPair() {
        return stpPair;
    }

    /**
     * @param stpPair the stpPair to set
     */
    public void setStpPair(StpPair stpPair) {
        this.stpPair = stpPair;
    }

    /**
     * @return the constraints
     */
    public Constraints getConstraints() {
        return constraints;
    }

    /**
     * @param constraints the constraints to set
     */
    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }
}
