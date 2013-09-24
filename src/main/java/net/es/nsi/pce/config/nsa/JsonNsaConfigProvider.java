package net.es.nsi.pce.config.nsa;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.es.nsi.pce.config.FileBasedConfigProvider;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;

public class JsonNsaConfigProvider  extends FileBasedConfigProvider implements ServiceInfoProvider, InitializingBean {
    private HashMap<String, NsaConfig> configs = new  HashMap<>();


    public void loadConfig() throws Exception {

        File configFile = new File(this.getFilename());
        // System.out.println("file: "+this.getFilename());

        String json = FileUtils.readFileToString(configFile);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, NsaConfig>>() {}.getType();

        configs = gson.fromJson(json, type);
        for (String nsaId : configs.keySet()) {
            NsaConfig cfg = configs.get(nsaId);
            // System.out.println(nsaId + " "+cfg.networkId);
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.loadConfig();
    }

    public NsaConfig getConfig(String nsaId) {
        return configs.get(nsaId);
    }


    public Set<String> getNsaIds() {
        return configs.keySet();
    }

    public ServiceInfo byNsaId(String nsaId) {
        if (!configs.containsKey(nsaId)) {
            return null;
        }
        ServiceInfo si = new ServiceInfo();
        NsaConfig cfg = configs.get(nsaId);
        si.setNetworkId(cfg.networkId);
        si.setNsaId(nsaId);
        si.setProviderUrl(cfg.providerUrl);
        return si;
    }

    public ServiceInfo byNetworkId(String networkId) {
        NsaConfig cfg = null;
        String nsaId = null;
        for (String tmp : configs.keySet()) {
            NsaConfig config = configs.get(tmp);
            if (config.networkId.equals(networkId)) {
                cfg = config;
                nsaId = tmp;
                break;
            }
        }
        // System.out.println("+++networkId:"+networkId+" nsa:"+nsaId);
        if (cfg == null) {
            // System.out.println("no cfg");
            return null;
        }

        ServiceInfo si = new ServiceInfo();
        si.setNetworkId(cfg.networkId);
        si.setProviderUrl(cfg.providerUrl);
        si.setNsaId(nsaId);
        return si;
    }




}
