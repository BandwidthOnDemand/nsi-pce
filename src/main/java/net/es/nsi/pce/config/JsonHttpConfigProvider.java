package net.es.nsi.pce.config;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;

public class JsonHttpConfigProvider {


    private HashMap<String, HttpConfig> configs = new HashMap<String, HttpConfig>();

    private static JsonHttpConfigProvider instance = new JsonHttpConfigProvider();



    public static JsonHttpConfigProvider getInstance() {
        return instance;
    }

    private JsonHttpConfigProvider() {
    }

    public void loadConfig(String filename) throws Exception {

        File configFile = new File(filename);
        String json = FileUtils.readFileToString(configFile);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, HttpConfig>>() {}.getType();

        configs = gson.fromJson(json, type);
    }

    public HttpConfig getConfig(String id) {
        return configs.get(id);
    }

}
