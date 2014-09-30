package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlTopologyType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import org.apache.http.client.utils.DateUtils;

/**
 * A factory class for generating NSI Network resource objects.
 *
 * @author hacksaw
 */
public class NsiNetworkFactory {
    private static final String NSI_ROOT_NETWORKS = "/networks/";

    /**
     * Create a NSI Network resource object from an NML JAXB object.
     *
     * @param nmlTopology NML JAXB object.
     * @param nsiNsa The NSI NSA resource object used for creating NSA references as well as setting discovered and version information.
     *
     * @return The new Network resource object.
     */
    public static NetworkType createNetworkType(NmlTopologyType nmlTopology, NsaType nsiNsa) {
        NetworkType nsiNetwork = new NetworkType();

        // Set the Id and naming information.
        nsiNetwork.setId(nmlTopology.getId());
        String name = nmlTopology.getName();
        if (name == null || name.isEmpty()) {
            name = nmlTopology.getId();
        }
        nsiNetwork.setName(name);

        // Create a direct reference to this Network object.
        nsiNetwork.setHref(NSI_ROOT_NETWORKS + nsiNetwork.getId());

        // Set the reference to the managing NSA.
        nsiNetwork.setNsa(nsiNsa.getSelf());

        // Use the managing NSA values for discovered and version.
        nsiNetwork.setDiscovered(nsiNsa.getDiscovered());
        nsiNetwork.setVersion(nsiNsa.getVersion());

        nsiNetwork.setSelf(NsiNetworkFactory.createResourceRefType(nsiNetwork));
        return nsiNetwork;
    }

    /**
     * Create a resource reference for the NSI Network resource.
     *
     * @param network Create a resource for this NSI Network resource object.
     *
     * @return The new resource reference.
     */
    public static ResourceRefType createResourceRefType(NetworkType network) {
        ResourceRefType nsaRef = new ResourceRefType();
        nsaRef.setId(network.getId());
        nsaRef.setHref(network.getHref());
        return nsaRef;
    }

    public static List<NetworkType> ifModifiedSince(String ifModifiedSince, List<NetworkType> networkList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<NetworkType> iter = networkList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }

        return networkList;
    }
}
