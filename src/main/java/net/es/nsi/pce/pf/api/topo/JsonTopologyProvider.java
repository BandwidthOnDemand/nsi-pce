package net.es.nsi.pce.pf.api.topo;


import net.es.nsi.pce.config.topo.JsonTopoConfigProvider;
import net.es.nsi.pce.config.topo.TopoStpConfig;

public class JsonTopologyProvider implements TopologyProvider {

    private static JsonTopologyProvider instance = new JsonTopologyProvider();
    private JsonTopologyProvider() {}
    public static JsonTopologyProvider getInstance() {
        return instance;
    }

    @Override
    public Topology getTopology() throws Exception {
        JsonTopoConfigProvider cfg = JsonTopoConfigProvider.getInstance();
        cfg.loadConfig();


        Topology topo = new Topology();
        for (String networkId : cfg.getNetworkIds()) {
            Network net = new Network();
            net.setNetworkId(networkId);
            topo.setNetwork(networkId, net);

            for (TopoStpConfig stpConfig : cfg.getConfig(networkId).stps) {
                Stp stp = new Stp();
                stp.setLocalId(stpConfig.localId);
                stp.setNetwork(net);
                net.put(stp.getLocalId(), stp);
            }
        }

        for (String networkId : cfg.getNetworkIds()) {
            for (TopoStpConfig stpConfig : cfg.getConfig(networkId).stps) {
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
}
