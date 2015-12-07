/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.schema;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.es.nsi.pce.jaxb.topology.NmlNetworkObject;
import net.es.nsi.pce.jaxb.topology.NmlTopologyType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class NmlTest {

    private final static QName _isReference_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "isReference");

    @Test
    public void testSuccess() throws Exception {
        NmlTopologyType topology = NmlParser.getInstance().xml2Nml("<nml:Topology id=\"urn:ogf:network:glif.is:2013:autogole-topology\"\n" +
"    xmlns:nml=\"http://schemas.ogf.org/nml/2013/05/base#\"\n" +
"    xmlns:nsi=\"http://schemas.ogf.org/nsi/2013/09/topology#\"\n" +
"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
"    <nml:Topology id=\"urn:ogf:network:ampath.net:2013:topology\" nsi:isReference=\"http://nsi.ampath.net:9080/NSI/topology/ampath.net:2013.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:aist.go.jp:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/aist.go.jp.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:czechlight.cesnet.cz:2013:topology:a-gole:testbed\" nsi:isReference=\"https://bod2.surfnet.nl/nsi-topology\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:es.net:2013:\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/es.net.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:geant.net:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/geant.net.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:jgn-x.jp:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/jgn-x.jp.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:sinet.ac.jp:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/sinet.ac.jp.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:kddilabs.jp:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/kddilabs.jp.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:krlight.net:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/krlight.net.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed\" nsi:isReference=\"https://bod.netherlight.net/nsi-topology\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:nordu.net:2013\" nsi:isReference=\"https://nsi.nordu.net:9443/NSI/topology/nordu.net:2013.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:pionier.net.pl:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/pionier.net.pl.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:southernlight.net:2013\" nsi:isReference=\"http://agole.ansp.br:9080/NSI/topology/southernlight.net:2013.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:uvalight.net:2013\" nsi:isReference=\"https://nsa.uvalight.net:9443/NSI/topology/uvalight.net:2013.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:grnet.gr:2013:topology\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/grnet.gr.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:manlan.internet2.edu:2013:\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/manlan.internet2.edu.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:surfnet.nl:1990:topology:surfnet6:production\" nsi:isReference=\"https://bod.surfnet.nl/nsi-topology\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:singaren.net:2013\" nsi:isReference=\"https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/goles/singaren.net.xml\" />\n" +
"    <nml:Topology id=\"urn:ogf:network:icair.org:2013\" nsi:isReference=\"http://pmri061.it.northwestern.edu:9080/NSI/topology/icair.org:2013.xml\" />\n" +
"</nml:Topology>");

        System.out.println(topology.getId());
        assertEquals("urn:ogf:network:glif.is:2013:autogole-topology", topology.getId());

        List<NmlNetworkObject> networkObjects = topology.getGroup();
        for (NmlNetworkObject networkObject : networkObjects) {
            if (networkObject instanceof NmlTopologyType) {
                NmlTopologyType innerTopology = (NmlTopologyType) networkObject;
                Map<QName, String> otherAttributes = innerTopology.getOtherAttributes();
                String isReference = otherAttributes.get(_isReference_QNAME);
                if (isReference != null && !isReference.isEmpty()) {
                    System.out.println("id: " + networkObject.getId() + ", isReference: " + isReference);
                }
                else {
                    System.out.println("id: " + networkObject.getId() + ", isReference: not present.");
                    fail();
                }
            }
            else {
                fail();
            }
        }
    }
}
