package net.es.nsi.pce.config.topo;

import java.util.Set;

/**
 * Holds a set of STPs derived from the network topology.
 *
 * @author hacksaw
 */

public class TopoNetworkConfig {
    private Set<TopoStpConfig> stps;

    public TopoNetworkConfig() {

    }

    /**
     * @return the stps
     */
    public Set<TopoStpConfig> getStps() {
        return stps;
    }

    /**
     * @param stps the stps to set
     */
    public void setStps(Set<TopoStpConfig> stps) {
        this.stps = stps;
    }

}
