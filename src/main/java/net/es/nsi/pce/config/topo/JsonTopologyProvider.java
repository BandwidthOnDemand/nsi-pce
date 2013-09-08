package net.es.nsi.pce.config.topo;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.es.nsi.pce.config.JsonConfigProvider;
import net.es.nsi.pce.pf.api.topo.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonTopologyProvider  extends JsonConfigProvider implements TopologyProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private HashMap<String, TopoNetworkConfig> configs = new HashMap<String, TopoNetworkConfig>();

    @Override
    public Topology getTopology() throws Exception {
        loadConfig();


        Topology topo = new Topology();
        for (String networkId : this.getNetworkIds()) {
            Network net = new Network();
            net.setNetworkId(networkId);
            topo.setNetwork(networkId, net);

            for (TopoStpConfig stpConfig : this.getConfig(networkId).stps) {
                Stp stp = new Stp();
                stp.setLocalId(stpConfig.localId);
                stp.setNetwork(net);
                net.put(stp.getLocalId(), stp);
            }
        }

        for (String networkId : this.getNetworkIds()) {
            for (TopoStpConfig stpConfig : this.getConfig(networkId).stps) {
                String rn = stpConfig.remoteNetworkId;
                String rl = stpConfig.remoteLocalId;
                if (rn != null && rl != null) {
                    Network rnet = topo.getNetwork(rn);
                    Stp rstp = rnet.getStp(rl);

                    String l = stpConfig.localId;
                    Network n = topo.getNetwork(networkId);
                    Stp stp = n.getStp(l);
                    StpConnection conn = new StpConnection();
                    conn.setA(stp);
                    conn.setZ(rstp);
                    n.getStpConnections().add(conn);

                }
            }
        }


        return topo;
    }

    @Override
    public void setTopologySource(String source) {
        this.setFilename(source);
    }
    @Override
    public void loadTopology() throws Exception {
        this.loadConfig();
    }

    @Override
    public void loadConfig() throws Exception {
        // System.out.println("loading "+this.getFilename());

        File configFile = new File(this.getFilename());
        String ap = configFile.getAbsolutePath();
        log.info("JsonTopologyProvider: loading topology " + ap);

        if (isFileUpdated(configFile)) {
            // System.out.println("file updated, loading");
            String json = FileUtils.readFileToString(configFile);
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, TopoNetworkConfig>>() {}.getType();

            configs = gson.fromJson(json, type);
        }

    }
    public TopoNetworkConfig getConfig(String networkId) {
        return configs.get(networkId);
    }

    @Override
    public Set<String> getNetworkIds() {
        return configs.keySet();
    }

}
