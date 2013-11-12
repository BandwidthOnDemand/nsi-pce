package net.es.nsi.pce.visualization;

import org.apache.commons.collections15.Transformer;

/**
 * Converts a Network vertex object into a Cartesian coordinate.
 * 
 * @author hacksaw
 */
class NetworkTransformer implements Transformer<NetworkVertex, CartesianCoordinates> {

    @Override
    public CartesianCoordinates transform(NetworkVertex network) {
        return NsaMap.getCoordinates(network.getId());
    }
    
}
