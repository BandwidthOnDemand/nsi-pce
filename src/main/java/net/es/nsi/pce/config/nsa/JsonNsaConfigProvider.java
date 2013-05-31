package net.es.nsi.pce.config.nsa;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.es.nsi.pce.config.JsonConfigProvider;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;

public class JsonNsaConfigProvider  extends JsonConfigProvider {
    private HashMap<String, NsaConfig> configs = new  HashMap<String, NsaConfig>();

    static JsonNsaConfigProvider instance;
    private JsonNsaConfigProvider() {

    }
    public static JsonNsaConfigProvider getInstance() {
        if (instance == null) {
            instance = new JsonNsaConfigProvider();
        }
        return instance;
    }

    public void loadConfig() throws Exception {

        File configFile = new File(this.getFilename());
        String json = FileUtils.readFileToString(configFile);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, NsaConfig>>() {}.getType();

        configs = gson.fromJson(json, type);
    }

    public NsaConfig getConfig(String nsaId) {
        return configs.get(nsaId);
    }

    public Set<String> getNsaIds() {
        return configs.keySet();
    }

    public NsaConfig getConfigFromNetworkId(String networkId) {
        for (NsaConfig config : configs.values()) {
            if (config.networkId.equals(networkId))
                return config;
        }
        return null;
    }

    public String getNsaId(String networkId) {
        for (String nsaId : configs.keySet()) {
            NsaConfig conf = configs.get(nsaId);
            if (conf.networkId.equals(networkId))
                return nsaId;
        }
        return null;
    }

}
