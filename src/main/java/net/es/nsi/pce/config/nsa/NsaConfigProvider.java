package net.es.nsi.pce.config.nsa;


import net.es.nsi.pce.config.ConfigProvider;

import java.util.Set;

public interface NsaConfigProvider extends ConfigProvider {

    public NsaConfig getConfig(String nsaId);

    public Set<String> getNsaIds();

    public NsaConfig getConfigFromNetworkId(String networkId);

    public String getNsaId(String networkId);
}
