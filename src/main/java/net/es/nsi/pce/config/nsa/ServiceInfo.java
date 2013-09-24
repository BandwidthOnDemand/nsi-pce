package net.es.nsi.pce.config.nsa;


public class ServiceInfo {
    protected String nsaId;
    protected String providerUrl;
    protected String networkId;
    
    public ServiceInfo() {

    }

    public String getNsaId() {
        return nsaId;
    }

    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }
}
