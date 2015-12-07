package net.es.nsi.pce.topology.model;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.jaxb.topology.NsaHolderType;
import net.es.nsi.pce.jaxb.topology.NsaNsaType;
import net.es.nsi.pce.jaxb.topology.NsaPeerRoleEnum;
import net.es.nsi.pce.jaxb.topology.NsaPeersWithType;
import net.es.nsi.pce.jaxb.topology.NsaType;
import net.es.nsi.pce.jaxb.topology.NsiResourceType;
import net.es.nsi.pce.jaxb.topology.PeerRoleEnum;
import net.es.nsi.pce.jaxb.topology.PeersWithType;
import net.es.nsi.pce.jaxb.topology.ReachabilityType;
import net.es.nsi.pce.jaxb.topology.ResourceRefType;
import net.es.nsi.pce.jaxb.topology.TopologyReachabilityType;
import net.es.nsi.pce.jaxb.topology.TopologyType;
import net.es.nsi.pce.jaxb.topology.VectorType;
import org.apache.http.client.utils.DateUtils;

/**
 * A factory class for generating NSI NSA resource objects.
 * @author hacksaw
 */
public class NsiNsaFactory {
    private static final net.es.nsi.pce.jaxb.topology.ObjectFactory tFactory = new net.es.nsi.pce.jaxb.topology.ObjectFactory();

    /**
     * Create a NSI NSA resource object from an NML JAXB object.
     *
     * @param nsa
     * @param baseURL
     * @return
     */
    public static NsaType createNsaType(NsaNsaType nsa, String baseURL) {

        NsaType nsiNsa = tFactory.createNsaType();
        nsiNsa.setId(nsa.getId());
        nsiNsa.setName(nsa.getName());
        nsiNsa.setVersion(nsa.getVersion());
        nsiNsa.setExpires(nsa.getExpires());

        nsiNsa.setHref(NsiPathURI.getURL(baseURL, NsiPathURI.NSI_ROOT_NSAS, nsiNsa.getId()));

        nsiNsa.setSoftwareVersion(nsa.getSoftwareVersion());
        nsiNsa.setStartTime(nsa.getStartTime());
        nsiNsa.setLocation(nsa.getLocation());
        nsiNsa.setAdminContact(nsa.getAdminContact());

        if (!nsa.getInterface().isEmpty()) {
            nsiNsa.getInterface().addAll(nsa.getInterface());
        }

        if (!nsa.getFeature().isEmpty()) {
            nsiNsa.getFeature().addAll(nsa.getFeature());
        }

        for (NsaPeersWithType peersWith : nsa.getPeersWith()) {
            PeersWithType peer = tFactory.createPeersWithType();

            peer.setId(peersWith.getValue().trim());
            peer.setHref(NsiPathURI.getURL(baseURL, NsiPathURI.NSI_ROOT_NSAS, peer.getId()));

            if (peersWith.getRole() == NsaPeerRoleEnum.PA) {
                peer.setRole(PeerRoleEnum.PA);
            }
            else {
                peer.setRole(PeerRoleEnum.RA);
            }

            nsiNsa.getPeersWith().add(peer);
        }

        // Pull the G0f3 reachability information out of the ANY as a custom
        // extension to the standard NSA elements.
        for (NsaHolderType holder : nsa.getOther()) {
            for (Object any : holder.getAny()) {
                if (any instanceof JAXBElement && ((JAXBElement) any).getValue() instanceof TopologyReachabilityType) {
                    @SuppressWarnings("unchecked")
                    JAXBElement<TopologyReachabilityType> element = (JAXBElement<TopologyReachabilityType>) any;
                    ReachabilityType reachability = tFactory.createReachabilityType();
                    // Assume there is only one networkId until Gof3 fix their
                    // modelling issue.
                    List<String> networkId = nsa.getNetworkId();
                    if (networkId.size() > 0) {
                        reachability.setId(networkId.get(0));
                    }
                    for (TopologyType topology : element.getValue().getTopology()) {
                        VectorType vector = tFactory.createVectorType();
                        vector.setId(topology.getId());
                        vector.setCost(topology.getCost());
                        reachability.getVector().add(vector);
                    }
                    nsiNsa.getReachability().add(reachability);
                }
                else {
                    nsiNsa.getAny().add(any);
                }
            }
        }

        // The network ResourceRefType is populated after the corresponding
        // networks are created.
        nsiNsa.setSelf(NsiNsaFactory.createResourceRefType(nsiNsa));

        return nsiNsa;
    }

    /**
     * Create a resource reference for the NSI Network resource.
     *
     * @param nsa Create a resource for this NSI NSA resource object.
     * @return
     */
    public static ResourceRefType createResourceRefType(NsaType nsa) {
        ResourceRefType nsaRef = new ResourceRefType();
        nsaRef.setId(nsa.getId());
        nsaRef.setHref(nsa.getHref());
        return nsaRef;
    }

    public static List<NsaType> ifModifiedSince(String ifModifiedSince, List<NsaType> nsaList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<NsaType> iter = nsaList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }

        return nsaList;
    }
}
