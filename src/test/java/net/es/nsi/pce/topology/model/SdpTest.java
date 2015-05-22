package net.es.nsi.pce.topology.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.es.nsi.pce.schema.NmlParser;
import net.es.nsi.pce.topology.jaxb.DdsCollectionType;
import net.es.nsi.pce.topology.jaxb.DdsDocumentListType;
import net.es.nsi.pce.topology.jaxb.DdsDocumentType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.provider.DdsWrapper;
import net.es.nsi.pce.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class SdpTest {
    private final static String DDS_NSA_DOCUMENT_TYPE = "vnd.ogf.nsi.nsa.v1+xml";
    private final static String DDS_TOPOLOGY_DOCUMENT_TYPE = "vnd.ogf.nsi.topology.v2+xml";
    private static Logger log;
    private static NsiTopology topology;

    @BeforeClass
    public static void setUp() throws Exception {
        // Configure Log4J and use logs from this point forward.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/log4j.xml"), 45 * 1000);
        log = LoggerFactory.getLogger(SdpTest.class);

        ObjectFactory factory = new ObjectFactory();
        DdsCollectionType collection = NmlParser.getInstance().parseDdsCollectionFromFile("src/test/resources/config/sdpTest.xml");

        DdsDocumentListType localNsaDocuments = factory.createDdsDocumentListType();
        DdsDocumentListType localTopologyDocuments = factory.createDdsDocumentListType();

        if (collection.getLocal() != null) {
            for (DdsDocumentType document : collection.getLocal().getDocument()) {
                if (DDS_NSA_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
                    localNsaDocuments.getDocument().add(document);
                }
                else if (DDS_TOPOLOGY_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
                    localTopologyDocuments.getDocument().add(document);
                }
            }
        }

        Map<String, DdsWrapper> nsaDocuments = new HashMap<>();
        Map<String, DdsWrapper> topologyDocuments = new HashMap<>();

        if (collection.getDocuments() != null) {
            for (DdsDocumentType document : collection.getDocuments().getDocument()) {
                if (DDS_NSA_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
                    DdsWrapper wrapper = new DdsWrapper();
                    wrapper.setDocument(document);
                    nsaDocuments.put(document.getId(), wrapper);
                }
                else if (DDS_TOPOLOGY_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
                    DdsWrapper wrapper = new DdsWrapper();
                    wrapper.setDocument(document);
                    topologyDocuments.put(document.getId(), wrapper);
                }
            }
        }

        NsiTopologyFactory nsiFactory = new NsiTopologyFactory();
        nsiFactory.setDefaultServiceType("http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE");
        nsiFactory.setBaseURL("http://localhost:8400/topology");
        topology = nsiFactory.createNsiTopology(localNsaDocuments, nsaDocuments, localTopologyDocuments, topologyDocuments);

        Collection<SdpType> unidirectionalSdp = NsiSdpFactory.createUnidirectionalSdp(topology.getStpMap());
        topology.addAllSdp(unidirectionalSdp);

        Collection<SdpType> bidirectionalSdps = NsiSdpFactory.createBidirectionalSdps(topology.getStpMap());
        topology.addAllSdp(bidirectionalSdps);
    }

    @Test
    public void goodSdpTest() throws Exception {
        // Unidirection SDP checks.
        assertNotNull(topology.getSdp("urn:ogf:network:icair.org:2013:topology:netherlight-out?vlan=1782::urn:ogf:network:netherlight.net:2013:production7:starlight-1-in?vlan=1782"));
        assertNotNull(topology.getSdp("urn:ogf:network:netherlight.net:2013:production7:starlight-1-out?vlan=1782::urn:ogf:network:icair.org:2013:topology:netherlight-in?vlan=1782"));

        // Bidirectional SDP check.
        assertNotNull(topology.getSdp("urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1782::urn:ogf:network:netherlight.net:2013:production7:starlight-1?vlan=1782"));
    }

    @Test
    public void badSdpTest() throws Exception {
        //assertNotNull(topology.getSdp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:netherlight-out?vlan=1779::urn:ogf:network:netherlight.net:2013:production7:czechlight-1-in?vlan=1779"));
        //assertNotNull(topology.getSdp("urn:ogf:network:netherlight.net:2013:production7:czechlight-1-out?vlan=1779::urn:ogf:network:czechlight.cesnet.cz:2013:topology:netherlight-in?vlan=1779"));
        assertNull(topology.getSdp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:netherlight?vlan=1779::urn:ogf:network:netherlight.net:2013:production7:czechlight-1?vlan=1779"));
    }
}
