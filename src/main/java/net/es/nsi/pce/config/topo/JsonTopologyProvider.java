package net.es.nsi.pce.config.topo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.es.nsi.pce.config.FileBasedConfigProvider;
import net.es.nsi.pce.pf.api.topo.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author hacksaw
 */
public class JsonTopologyProvider  extends FileBasedConfigProvider implements TopologyProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Map holding the network topology indexed by network Id.
    private HashMap<String, TopoNetworkConfig> configs = new HashMap<String, TopoNetworkConfig>();

    @Override
    public Topology getTopology() throws Exception {
        loadTopology();

        Topology topo = new Topology();
        for (String networkId : this.getNetworkIds()) {
            Network net = new Network();
            net.setNetworkId(networkId);
            topo.addNetwork(net);

            for (TopoStpConfig stpConfig : this.getConfig(networkId).getStps()) {
                Stp stp = new Stp();
                stp.setLocalId(stpConfig.getLocalId());
                stp.setNetwork(net);
                net.put(stp.getLocalId(), stp);
            }
        }

        // Compute the SDP links.
        for (String networkId : this.getNetworkIds()) {
            for (TopoStpConfig stpConfig : this.getConfig(networkId).getStps()) {
                String rn = stpConfig.getRemoteNetworkId();
                String rl = stpConfig.getRemoteLocalId();
                if (rn != null && rl != null) {
                    Network rnet = topo.getNetworkById(rn);
                    Stp rstp = rnet.getStp(rl);

                    String l = stpConfig.getLocalId();
                    Network n = topo.getNetworkById(networkId);
                    Stp stp = n.getStp(l);
                    Sdp conn = new Sdp();
                    conn.setA(stp);
                    conn.setZ(rstp);
                    topo.getSdpLinks().put(conn.getId(), conn);

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
    public String getTopologySource() {
        return this.getFilename();
    }
        
    @Override
    public void loadTopology() throws Exception {
        
        // Load individual NML files.
        this.loadConfig();
    }

    @Override
    public void loadConfig() throws Exception {

        File configFile = new File(this.getFilename());
        String ap = configFile.getAbsolutePath();
        log.info("JsonTopologyProvider: loading topology " + ap);

        if (isFileUpdated()) {
            log.info("loadConfig: file updated, loading...");
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

    /**
     * @return the auditInterval
     */
    @Override
    public long getAuditInterval() {
        return 10*60*1000;
    }
}
