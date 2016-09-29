package net.es.nsi.pce.pf.api;

import com.google.common.base.Objects;
import java.io.Serializable;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;

/**
 *
 * @author hacksaw
 */
@SuppressWarnings("serial")
public class PathSegment implements Serializable {
    private String a;
    private String z;
    private String networkId;
    private String nsaId;
    private String csProviderURL;
    private AttrConstraints constraints = new AttrConstraints();

    protected PathSegment(Builder builder) {
        this.a = builder.a;
        this.z = builder.z;
        this.networkId = builder.networkId;
        this.nsaId = builder.nsaId;
        this.csProviderURL = builder.csProviderURL;
        this.constraints = builder.constraints;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("stpA", getA()).add("stpZ", getZ()).add("networkId", getNetworkId()).add("nsaId", getNsaId()).add("providerUrl", getCsProviderURL()).toString();
    }

    /**
     * @return the a
     */
    public String getA() {
        return a;
    }

    /**
     * @param a the a to set
     */
    public void setA(String a) {
        this.a = a;
    }

    /**
     * @return the z
     */
    public String getZ() {
        return z;
    }

    /**
     * @param z the z to set
     */
    public void setZ(String z) {
        this.z = z;
    }

    /**
     * @return the networkId
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @param networkId the networkId to set
     */
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
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
     * @return the constraints
     */
    public AttrConstraints getConstraints() {
        return constraints;
    }

    /**
     * @param constraints the constraints to set
     */
    public void setConstraints(AttrConstraints constraints) {
        this.constraints = constraints;
    }
    
    public static class Builder {
        private String a;
        private String z;
        private String networkId;
        private String nsaId;
        private String csProviderURL;
        private AttrConstraints constraints = new AttrConstraints();

        public Builder() {}

        public Builder withA(String a) {
            this.a = a;
            return this;
        }
        
        public Builder withZ(String z) {
            this.z = z;
            return this;
        }
        
        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withNsaId(String nsaId) {
            this.nsaId = nsaId;
            return this;
        }
        
        public Builder withCsProviderURL(String csProviderURL) {
            this.csProviderURL = csProviderURL;
            return this;
        }
        
        public Builder withConstraints(AttrConstraints constraints) {
            this.constraints = constraints;
            return this;
        }

        public PathSegment build() {
            return new PathSegment(this);
        }
    }
}
