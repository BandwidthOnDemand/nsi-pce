/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.agole;

import akka.actor.UntypedActor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.discovery.jaxb.AnyType;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.FeatureType;
import net.es.nsi.pce.discovery.jaxb.InterfaceType;
import net.es.nsi.pce.discovery.jaxb.LocationType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.NsaType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import net.es.nsi.pce.discovery.jaxb.NmlLifeTimeType;
import net.es.nsi.pce.discovery.jaxb.NmlLocationType;
import net.es.nsi.pce.discovery.jaxb.NmlNSARelationType;
import net.es.nsi.pce.discovery.jaxb.NmlNSAType;
import net.es.nsi.pce.discovery.jaxb.NmlServiceType;
import net.es.nsi.pce.discovery.jaxb.NmlTopologyType;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.schema.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class AgoleDiscoveryActor extends UntypedActor {
   
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof AgoleDiscoveryMsg) {
            AgoleDiscoveryMsg message = (AgoleDiscoveryMsg) msg;
            
            // Read the NML topology document.
            if (discoverTopology(message) == false) {
                // No update so return.
                return;
            }
            
            // Send an updated discovery message back to the router.
            getSender().tell(message, getSelf());
        } else {
            unhandled(msg);
        }
    }
 
    private boolean discoverTopology(AgoleDiscoveryMsg message) {
        String id = message.getId();
        String url = message.getTopologyURL();

        log.debug("discover: topology id=" + id + ", url=" + url);
        
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        AgoleTopologyReader topologyReader = new AgoleTopologyReader(id, url, message.getTopologyLastModifiedTime());
        NmlNSAType nsa;
        try {
            nsa = topologyReader.readNsaTopology();
        } catch (Exception ex) {
            log.error("discoverTopology: failed to read topology for id=" + id + ", url=" + url, ex);
            return false;
        }
        
        if (nsa == null) {
            log.debug("discoverTopology: Topology document not modified for id=" + id + ", url=" + url);
            return false;
        }
        
        XMLGregorianCalendar lastDiscovered;
        try {
            lastDiscovered = XmlUtilities.xmlGregorianCalendar();
        } catch (DatatypeConfigurationException ex) {
            log.error("discoverTopology: Failed to create a lastDiscovered value, id=" + nsa.getId(), ex);
            return false;
        }
        
        // We need to create both the NSA Discovery and Topology documents from
        // the contents of this single document.
        NsaType nsaDocument = parseNsa(nsa);
        if (nsaDocument == null || !addNsaDocument(nsaDocument, lastDiscovered)) {
            return false;
        }

        // Update the lastModified time and nsaId in the message so we can sent
        // it back to the router to update the master list.
        message.setTopologyLastModifiedTime(topologyReader.getLastModifiedTime());
        message.setNsaId(nsa.getId());

        Collection<NmlTopologyType> nmlDocuments = parseTopology(nsa, nsaDocument);
        for (NmlTopologyType nmlDocument : nmlDocuments) {
            try {
                addTopologyDocument(nmlDocument, lastDiscovered, nsa.getId());
            }
            catch (Exception ex) {
                log.error("discoverTopology: Failed to topology document, nsaId=" + nsa.getId() + ", networkId=" + nmlDocument.getId());
            }
        }

        // Now we retrieve the associated topology document.
        log.debug("discoverTopology: existing.");
        
        return true;
    }
    
    private boolean addNsaDocument(NsaType nsa, XMLGregorianCalendar discovered) {
        // Now we add the NSA document into the DDS.
        DocumentType document = factory.createDocumentType();

        // Set the naming attributes.
        document.setId(nsa.getId());
        document.setType(NsiConstants.NSI_DOC_TYPE_NSA_V1);
        document.setNsa(nsa.getId());

        // We need the version of the document.
        XMLGregorianCalendar version = nsa.getVersion();
        if (version == null || !version.isValid()) {
            
        }
        else {
            document.setVersion(version);
        }

        // If there is no expires time specified then it is infinite.
        if (nsa.getExpires() == null || !nsa.getExpires().isValid()) {
            // No expire value provided so make one.
            Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
            } catch (DatatypeConfigurationException ex) {
                log.error("discover: NSA document does not contain an expires date, id=" + nsa.getId());
                return false;
            }

            document.setExpires(xmlGregorianCalendar);
        }
        else {
            document.setExpires(nsa.getExpires());
        }

        // Add the NSA document into the entry.
        AnyType any = factory.createAnyType();
        any.getAny().add(factory.createNsa(nsa));
        document.setContent(any);
        
        // Try to add the document as a notification of document change.
        NotificationType notify = factory.createNotificationType();
        notify.setEvent(DocumentEventType.ALL);
        notify.setDiscovered(discovered);
        notify.setDocument(document);
        
        DdsProvider.getInstance().processNotification(notify);
        
        return true;
    }
    
    private boolean addTopologyDocument(NmlTopologyType topology, XMLGregorianCalendar discovered, String nsaId) {
        
        // Now we add the NSA document into the DDS.
        DocumentType document = factory.createDocumentType();

        // Set the naming attributes.
        document.setId(topology.getId());
        document.setType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
        document.setNsa(nsaId);

        // We need the version of the document.
        document.setVersion(topology.getVersion());

        // If there is no expires time specified then it is infinite.
        if (topology.getLifetime() == null || topology.getLifetime().getEnd() == null || !topology.getLifetime().getEnd().isValid()) {
            // No expire value provided so make one.
            Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
            } catch (DatatypeConfigurationException ex) {
                log.error("discover: Topology document does not contain an expires date, id=" + topology.getId());
                return false;
            }

            document.setExpires(xmlGregorianCalendar);
            NmlLifeTimeType lifetime = factory.createNmlLifeTimeType();
            lifetime.setStart(topology.getVersion());
            lifetime.setEnd(xmlGregorianCalendar);
            topology.setLifetime(lifetime);
        }
        else {
            document.setExpires(topology.getLifetime().getEnd());
        }

        // Add the NSA document into the entry.
        AnyType any = factory.createAnyType();
        any.getAny().add(factory.createTopology(topology));
        document.setContent(any);
        
        // Try to add the document as a notification of document change.
        NotificationType notify = factory.createNotificationType();
        notify.setEvent(DocumentEventType.ALL);
        notify.setDiscovered(discovered);
        notify.setDocument(document);
        
        DdsProvider.getInstance().processNotification(notify);
        
        return true;
    }
    
    private NsaType parseNsa(NmlNSAType nsa) {
        // We need to create both the NSA Discovery and Topology documents from
        // the contents of this single document.
        NsaType nsaDocument = factory.createNsaType();
        nsaDocument.setId(nsa.getId());
        nsaDocument.setVersion(nsa.getVersion());
        nsaDocument.setName(nsa.getName());
        if (nsa.getLifetime() != null) {
            nsaDocument.setExpires(nsa.getLifetime().getEnd());
        }
        
        NmlLocationType location = nsa.getLocation();
        if (location != null) {
            LocationType loc = factory.createLocationType();
            loc.setAltitude(location.getAlt());
            loc.setLatitude(location.getLat());
            loc.setLongitude(location.getLong());
            loc.setName(location.getName());
            loc.setUnlocode(location.getUnlocode());
            nsaDocument.setLocation(loc);
        }
        
        // Add the uPA NSA feature.
        FeatureType upa = factory.createFeatureType();
        upa.setType(NsiConstants.NSI_CS_UPA);
        List<FeatureType> feature = nsaDocument.getFeature();
        feature.add(upa);
        
        //nsaDocument.setAdminContact(null);
        
        // Parse the NML peersWith relationship.
        try {
            nsaDocument.getPeersWith().addAll(parsePeersWith(nsa.getRelation()));
        }
        catch (Exception ex) {
            // Ignore the error for now.
            log.error("discoverTopology: failed to add NML peersWith relationship.", ex);
        }
        
        // Parse the NML Service element into an interface element.
        try {
            nsaDocument.getInterface().addAll(parseService(nsa.getService()));
        }
        catch (Exception ex) {
            // Ignore the error for now.
            log.error("discoverTopology: failed to add NML Service.", ex);
        }
        
        // We pull the networkId out of the <Topology> elements.
        List<String> networkId = nsaDocument.getNetworkId();
        for (NmlTopologyType topology : nsa.getTopology()) {
            networkId.add(topology.getId().trim());
        }
        
        return nsaDocument;
    }
    
    private Collection<NmlTopologyType> parseTopology(NmlNSAType nmlNsa, NsaType nsaDocument) {
        List<NmlTopologyType> topologies = nmlNsa.getTopology();
        for (NmlTopologyType topology : topologies) {
            if (topology.getVersion() == null || !topology.getVersion().isValid()) {
                topology.setVersion(nsaDocument.getVersion());
            }
            if (topology.getLifetime() == null || topology.getLifetime().getEnd() == null || !topology.getLifetime().getEnd().isValid()) {
                NmlLifeTimeType lifetime = factory.createNmlLifeTimeType();
                lifetime.setEnd(nsaDocument.getExpires());
                topology.setLifetime(lifetime);
            }
        }
        return topologies;
    }

    private Collection<String> parsePeersWith(List<NmlNSARelationType> relationList) {
        List<String> peersWith = new ArrayList<>();
        for (NmlNSARelationType relation : relationList) {
            if (NsiConstants.NML_PEERSWITH_RELATION.equalsIgnoreCase(relation.getType())) {
                for (NmlNSAType nsa : relation.getNSA()) {
                    peersWith.add(nsa.getId().trim());
                }
            }
        }
        
        return peersWith;
    }

    private List<InterfaceType> parseService(List<NmlServiceType> services) {
        List<InterfaceType> interfaceList = new ArrayList<>();
        for (NmlServiceType service : services) {
            InterfaceType aInterface = factory.createInterfaceType();
            aInterface.setHref(service.getLink().trim());
            aInterface.setType(NsiConstants.NSI_CS_PROVIDER_V2);
            interfaceList.add(aInterface);
        }
        
        return interfaceList;
    }
}