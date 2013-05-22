package net.es.nsi.pce.topo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;

public class JsonTopoConfigProvider {
    private HashMap<String, TopoNetworkConfig> configs = new HashMap<String, TopoNetworkConfig>();
    static JsonTopoConfigProvider instance;
    private JsonTopoConfigProvider() {

    }
    public static JsonTopoConfigProvider getInstance() {
        if (instance == null) {
            instance = new JsonTopoConfigProvider();
        }
        return instance;
    }


    public void loadConfig(String filename) throws Exception {

        File configFile = new File(filename);

        String json = FileUtils.readFileToString(configFile);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, TopoNetworkConfig>>() {}.getType();

        configs = gson.fromJson(json, type);
    }

    public TopoNetworkConfig getConfig(String networkId) {
        return configs.get(networkId);
    }

    public Set<String> getNetworkIds() {
        return configs.keySet();
    }


}
