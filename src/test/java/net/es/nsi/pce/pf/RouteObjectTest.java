/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.OrderedStpType;
import net.es.nsi.pce.path.jaxb.StpListType;
import net.es.nsi.pce.schema.NmlParser;
import net.es.nsi.pce.topology.jaxb.DdsCollectionType;
import net.es.nsi.pce.topology.jaxb.DdsDocumentListType;
import net.es.nsi.pce.topology.jaxb.DdsDocumentType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.model.NsiSdpFactory;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.model.NsiTopologyFactory;
import net.es.nsi.pce.topology.model.SdpTest;
import net.es.nsi.pce.topology.provider.DdsWrapper;
import net.es.nsi.pce.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RouteObjectTest {
    private final static String DDS_NSA_DOCUMENT_TYPE = "vnd.ogf.nsi.nsa.v1+xml";
    private final static String DDS_TOPOLOGY_DOCUMENT_TYPE = "vnd.ogf.nsi.topology.v2+xml";
    private static Logger log;
    private static final ObjectFactory factory = new ObjectFactory();
    private static NsiTopology topology;

    public RouteObjectTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @BeforeClass
    public static void setUp() throws Exception {
        // Configure Log4J and use logs from this point forward.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/log4j.xml"), 45 * 1000);
        log = LoggerFactory.getLogger(SdpTest.class);

        DdsCollectionType collection = NmlParser.getInstance().parseDdsCollectionFromFile("src/test/resources/config/eroTest.xml");

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
        nsiFactory.setDefaultServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
        nsiFactory.setBaseURL("http://localhost:8400/topology");
        topology = nsiFactory.createNsiTopology(localNsaDocuments, nsaDocuments, localTopologyDocuments, topologyDocuments);

        Collection<SdpType> unidirectionalSdp = NsiSdpFactory.createUnidirectionalSdp(topology.getStpMap());
        topology.addAllSdp(unidirectionalSdp);

        Collection<SdpType> bidirectionalSdps = NsiSdpFactory.createBidirectionalSdps(topology.getStpMap());
        topology.addAllSdp(bidirectionalSdps);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getRoutes method, of class RouteObject.
     */
    @Test
    public void testGetRoutes() {
        System.out.println("getRoutes");
        SimpleStp srcStpId = new SimpleStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:brno?vlan=1790");
        SimpleStp dstStpId = new SimpleStp("urn:ogf:network:icair.org:2013:topology:showfloor?vlan=1790");
        net.es.nsi.pce.path.jaxb.ObjectFactory pathFactory = new net.es.nsi.pce.path.jaxb.ObjectFactory();
        StpListType ero = pathFactory.createStpListType();

        // Set internal STP for first domain.
        OrderedStpType internal0 = pathFactory.createOrderedStpType();
        internal0.setOrder(0);
        internal0.setStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:internal1");
        ero.getOrderedSTP().add(internal0);

        OrderedStpType internal1 = pathFactory.createOrderedStpType();
        internal1.setOrder(1);
        internal1.setStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:internal2");
        ero.getOrderedSTP().add(internal1);

        // The interdomain edge for source domain.
        OrderedStpType edge = pathFactory.createOrderedStpType();
        edge.setOrder(2);
        edge.setStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:netherlight?vlan=1780-1790");
        ero.getOrderedSTP().add(edge);

        // An interdomain port in another domain.
        OrderedStpType interdomainStpType = pathFactory.createOrderedStpType();
        interdomainStpType.setOrder(3);
        interdomainStpType.setStp("urn:ogf:network:netherlight.net:2013:production7:starlight-1?vlan=1791");
        ero.getOrderedSTP().add(interdomainStpType);

        RouteObject instance = new RouteObject(topology, srcStpId, dstStpId, DirectionalityType.BIDIRECTIONAL, Optional.of(ero));
        for (Route route : instance.getRoutes()) {
            System.out.println(route.getBundleA().getSimpleStp());
            for (OrderedStpType stp : route.getInternalStp()) {
                System.out.println("internal: " + stp.getOrder() + ", stp=" + stp.getStp());
            }
            System.out.println(route.getBundleZ().getSimpleStp());
        }

        /* Expected results:
         *  urn:ogf:network:czechlight.cesnet.cz:2013:topology:brno?vlan=1790
         *  internal: 0, stp=urn:ogf:network:czechlight.cesnet.cz:2013:topology:internal1
         *  internal: 1, stp=urn:ogf:network:czechlight.cesnet.cz:2013:topology:internal2
         *  urn:ogf:network:czechlight.cesnet.cz:2013:topology:netherlight?vlan=1780-1790
         *
         *  urn:ogf:network:netherlight.net:2013:production7:czechlight-1?vlan=1780-1790
         *  urn:ogf:network:netherlight.net:2013:production7:starlight-1?vlan=1791
         *
         *  urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1791
         *  urn:ogf:network:icair.org:2013:topology:showfloor?vlan=1790
         */
        List<Route> expResult = new ArrayList<>();
        Route route = new Route();
        StpTypeBundle bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:brno?vlan=1790"), DirectionalityType.BIDIRECTIONAL);
        StpTypeBundle bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:netherlight?vlan=1780-1790"), DirectionalityType.BIDIRECTIONAL);
        route.setBundleA(bundleA);
        route.setBundleZ(bundleZ);
        route.addInternalStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:internal1");
        route.addInternalStp("urn:ogf:network:czechlight.cesnet.cz:2013:topology:internal2");
        expResult.add(route);

        route = new Route();
        bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:czechlight-1?vlan=1780-1790"), DirectionalityType.BIDIRECTIONAL);
        bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:starlight-1?vlan=1791"), DirectionalityType.BIDIRECTIONAL);
        route.setBundleA(bundleA);
        route.setBundleZ(bundleZ);
        expResult.add(route);

        route = new Route();
        bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:icair.org:2013:topology:netherlight?vlan=1791"), DirectionalityType.BIDIRECTIONAL);
        bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:icair.org:2013:topology:showfloor?vlan=1790"), DirectionalityType.BIDIRECTIONAL);
        route.setBundleA(bundleA);
        route.setBundleZ(bundleZ);
        expResult.add(route);

        List<Route> result = instance.getRoutes();
        assertEquals(expResult, result);
    }
}
