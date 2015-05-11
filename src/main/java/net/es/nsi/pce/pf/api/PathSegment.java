package net.es.nsi.pce.pf.api;

import java.io.Serializable;

import com.google.common.base.Objects;

import net.es.nsi.pce.pf.api.cons.AttrConstraints;

/**
 *
 * @author hacksaw
 */
@SuppressWarnings("serial")
public class PathSegment implements Serializable {

    private final StpPair stpPair;
    private String nsaId;
    private String csProviderURL;
    private AttrConstraints constraints = new AttrConstraints();

    public PathSegment(StpPair stpPair) {
        this.stpPair = stpPair;
    }

    public PathSegment withNsa(String nsaId, String providerUrl) {
        this.nsaId = nsaId;
        this.csProviderURL = providerUrl;
        return this;
    }

    public String getNsaId() {
        return nsaId;
    }

    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }

    public String getCsProviderURL() {
        return csProviderURL;
    }

    public void setCsProviderURL(String csProviderURL) {
        this.csProviderURL = csProviderURL;
    }

    public StpPair getStpPair() {
        return stpPair;
    }

    public AttrConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(AttrConstraints constraints) {
        this.constraints = constraints;
    }

    public String toString() {
        return Objects.toStringHelper(this).add("stpPair", stpPair).add("nsaId", nsaId).add("providerUrl", csProviderURL).toString();
    }
}
