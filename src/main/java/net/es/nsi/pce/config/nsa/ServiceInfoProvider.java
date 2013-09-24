package net.es.nsi.pce.config.nsa;

import java.util.Set;

public interface ServiceInfoProvider {
    public ServiceInfo byNsaId(String nsaId);
    public ServiceInfo byNetworkId(String networkId);
    public Set<String> getNsaIds();
}
