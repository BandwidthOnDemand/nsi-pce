package net.es.nsi.pce.visualization;

import net.es.nsi.pce.pf.api.topo.Network;
import org.apache.commons.collections15.Transformer;

/**
 * Converts a Network vertex object into a Cartesian coordinate.
 * 
 * @author hacksaw
 */
class NetworkTransformer implements Transformer<Network, CartesianCoordinates> {

    @Override
    public CartesianCoordinates transform(Network network) {
        return NsaMap.getCoordinates(network.getNetworkId());
    }
    
}
