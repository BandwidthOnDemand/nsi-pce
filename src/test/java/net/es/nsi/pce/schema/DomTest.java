package net.es.nsi.pce.schema;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.pce.jaxb.topology.NmlTopologyType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author hacksaw
 */
public class DomTest {
    @Test
    public void xml2domTest() throws ParserConfigurationException, SAXException, IOException, JAXBException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns3:Topology xmlns:ns3=\"http://schemas.ogf.org/nml/2013/05/base#\" xmlns:ns2=\"http://schemas.ogf.org/nsi/2013/09/topology#\" xmlns:ns4=\"http://schemas.ogf.org/nsi/2013/12/services/definition\" xmlns:ns5=\"http://schemas.ogf.org/nml/2014/01/ethernet\" id=\"urn:ogf:network:deic.dk:2013:topology\" version=\"2015-12-02T17:37:45.252Z\"><ns3:name>deic.dk</ns3:name><ns3:Lifetime><ns3:start>2015-12-02T17:37:45.252Z</ns3:start><ns3:end>2016-12-01T12:38:08.483-05:00</ns3:end></ns3:Lifetime><ns3:BidirectionalPort id=\"urn:ogf:network:deic.dk:2013:topology:funet-geant\"><ns3:name>funet-geant</ns3:name><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:funet-geant-in\"/><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:funet-geant-out\"/></ns3:BidirectionalPort><ns3:BidirectionalPort id=\"urn:ogf:network:deic.dk:2013:topology:StoragePort\"><ns3:name>StoragePort</ns3:name><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:StoragePort-in\"/><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:StoragePort-out\"/></ns3:BidirectionalPort><ns3:BidirectionalPort id=\"urn:ogf:network:deic.dk:2013:topology:iperfPort\"><ns3:name>iperfPort</ns3:name><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:iperfPort-in\"/><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:iperfPort-out\"/></ns3:BidirectionalPort><ns4:serviceDefinition id=\"urn:ogf:network:deic.dk:2013:topologyServiceDefinition:EVTS.A-GOLE\"><name>GLIF Automated GOLE Ethernet VLAN Transfer Service</name><serviceType>http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE</serviceType></ns4:serviceDefinition><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\"><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:funet-geant-out\"><ns3:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2015-2025</ns3:LabelGroup><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#isAlias\"><ns3:PortGroup id=\"urn:ogf:network:geant.net:2013:topology:deic-geant-in\"/></ns3:Relation></ns3:PortGroup></ns3:Relation><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\"><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:funet-geant-in\"><ns3:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2015-2025</ns3:LabelGroup><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#isAlias\"><ns3:PortGroup id=\"urn:ogf:network:geant.net:2013:topology:deic-geant-out\"/></ns3:Relation></ns3:PortGroup></ns3:Relation><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\"><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:StoragePort-out\"><ns3:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2015-2025</ns3:LabelGroup></ns3:PortGroup></ns3:Relation><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\"><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:StoragePort-in\"><ns3:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2015-2025</ns3:LabelGroup></ns3:PortGroup></ns3:Relation><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\"><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:iperfPort-out\"><ns3:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">1-4094</ns3:LabelGroup></ns3:PortGroup></ns3:Relation><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\"><ns3:PortGroup id=\"urn:ogf:network:deic.dk:2013:topology:iperfPort-in\"><ns3:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">1-4094</ns3:LabelGroup></ns3:PortGroup></ns3:Relation><ns3:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasService\"><ns3:SwitchingService id=\"urn:ogf:network:deic.dk:2013:topologyServiceDomain:a-gole:testbed:A-GOLE-EVTS\" labelSwapping=\"true\" labelType=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\"><ns4:serviceDefinition id=\"urn:ogf:network:deic.dk:2013:topologyServiceDefinition:EVTS.A-GOLE\"><name>GLIF Automated GOLE Ethernet VLAN Transfer Service</name><serviceType>http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE</serviceType></ns4:serviceDefinition></ns3:SwitchingService></ns3:Relation></ns3:Topology>";
        Document xml2Dom = DomParser.xml2Dom(xml);
        assertNotNull(xml2Dom);

        NmlTopologyType nmlDom = NmlParser.getInstance().dom2Nml(xml2Dom);
        assertNotNull(nmlDom);

        NmlTopologyType nmlJaxb = NmlParser.getInstance().xml2Nml(xml);
        assertNotNull(nmlJaxb);

        assertEquals(nmlDom.getId(), nmlJaxb.getId());
    }
}
