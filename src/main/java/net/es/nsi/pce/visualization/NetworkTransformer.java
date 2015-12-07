package net.es.nsi.pce.visualization;

import com.google.common.base.Optional;
import net.es.nsi.pce.jaxb.topology.NsaLocationType;
import net.es.nsi.pce.jaxb.topology.NsaType;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.apache.commons.collections15.Transformer;

/**
 * Converts a Network vertex object into a Cartesian coordinate.
 *
 * @author hacksaw
 */
class NetworkTransformer implements Transformer<NetworkVertex, CartesianCoordinates> {
    private final NsiTopology topology;

    public NetworkTransformer(NsiTopology topology) {
        this.topology = topology;
    }

    @Override
    public CartesianCoordinates transform(NetworkVertex network) {
        NsaType nsa = topology.getNsa(network.getNsa().getId());
        Optional<NsaLocationType> location = Optional.fromNullable(nsa.getLocation());

        CartesianCoordinates coords;

        if (location.isPresent()) {
            coords = Display.getCoordinates(location.get().getLongitude(),
                location.get().getLatitude());
        }
        else {
            coords = Display.getRandomCoordinates();
        }

        return coords;
    }

}
