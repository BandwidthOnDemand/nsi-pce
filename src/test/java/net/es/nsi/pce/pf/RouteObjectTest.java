/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.es.nsi.pce.jaxb.dds.CollectionType;
import net.es.nsi.pce.jaxb.dds.DocumentListType;
import net.es.nsi.pce.jaxb.dds.DocumentType;
import net.es.nsi.pce.jaxb.dds.ObjectFactory;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.pf.route.Route;
import net.es.nsi.pce.pf.route.RouteObject;
import net.es.nsi.pce.pf.route.StpTypeBundle;
import net.es.nsi.pce.pf.simple.SimpleStp;
import net.es.nsi.pce.schema.DdsParser;
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

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  public RouteObjectTest() throws Exception {
    topology = setUp("src/test/resources/config/eroTest2.xml");
  }

  public static NsiTopology setUp(String filename) throws Exception {
    // Configure Log4J and use logs from this point forward.
    DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/log4j.xml"), 45 * 1000);
    log = LoggerFactory.getLogger(SdpTest.class);

    CollectionType collection = DdsParser.getInstance().readCollection(filename);

    DocumentListType localNsaDocuments = factory.createDocumentListType();
    DocumentListType localTopologyDocuments = factory.createDocumentListType();

    if (collection.getLocal() != null) {
      for (DocumentType document : collection.getLocal().getDocument()) {
        if (DDS_NSA_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
          localNsaDocuments.getDocument().add(document);
        } else if (DDS_TOPOLOGY_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
          localTopologyDocuments.getDocument().add(document);
        }
      }
    }

    Map<String, DdsWrapper> nsaDocuments = new HashMap<>();
    Map<String, DdsWrapper> topologyDocuments = new HashMap<>();

    if (collection.getDocuments() != null) {
      for (DocumentType document : collection.getDocuments().getDocument()) {
        if (DDS_NSA_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
          DdsWrapper wrapper = new DdsWrapper();
          wrapper.setDocument(document);
          nsaDocuments.put(document.getId(), wrapper);
        } else if (DDS_TOPOLOGY_DOCUMENT_TYPE.equalsIgnoreCase(document.getType())) {
          DdsWrapper wrapper = new DdsWrapper();
          wrapper.setDocument(document);
          topologyDocuments.put(document.getId(), wrapper);
        }
      }
    }

    NsiTopologyFactory nsiFactory = new NsiTopologyFactory();
    nsiFactory.setDefaultServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
    nsiFactory.setBaseURL("http://localhost:8400/topology");
    NsiTopology top = nsiFactory.createNsiTopology(localNsaDocuments,
            nsaDocuments, localTopologyDocuments, topologyDocuments);

    Collection<SdpType> unidirectionalSdp = NsiSdpFactory.createUnidirectionalSdp(top.getStpMap());
    top.addAllSdp(unidirectionalSdp);

    Collection<SdpType> bidirectionalSdps = NsiSdpFactory.createBidirectionalSdps(top.getStpMap());
    top.addAllSdp(bidirectionalSdps);

    return top;
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getRoutes method, of class RouteObject.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetRoutes() throws Exception {
    System.out.println("getRoutes");
    SimpleStp srcStpId = new SimpleStp("urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:+?vlan=1000");
    SimpleStp dstStpId = new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:cern-1?vlan=3989");
    net.es.nsi.pce.jaxb.path.ObjectFactory pathFactory = new net.es.nsi.pce.jaxb.path.ObjectFactory();
    StpListType ero = pathFactory.createStpListType();

    // Set internal STP for first domain.
    OrderedStpType internal0 = pathFactory.createOrderedStpType();
    internal0.setOrder(0);
    internal0.setStp("urn:ogf:network:es.net:2013::internal1");
    ero.getOrderedSTP().add(internal0);

    OrderedStpType internal1 = pathFactory.createOrderedStpType();
    internal1.setOrder(1);
    internal1.setStp("urn:ogf:network:es.net:2013::internal2");
    ero.getOrderedSTP().add(internal1);

    // The interdomain edge for source domain.
    OrderedStpType edge = pathFactory.createOrderedStpType();
    edge.setOrder(2);
    edge.setStp("urn:ogf:network:es.net:2013::star-cr5:10_1_8:+?vlan=2-4094");
    ero.getOrderedSTP().add(edge);

    // An interdomain port in another domain.
    OrderedStpType interdomainStpType = pathFactory.createOrderedStpType();
    interdomainStpType.setOrder(3);
    interdomainStpType.setStp("urn:ogf:network:icair.org:2013:topology:nl-cern1?vlan=3985");
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
urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:+?vlan=1000
internal: 0, stp=urn:ogf:network:es.net:2013::internal1
internal: 1, stp=urn:ogf:network:es.net:2013::internal2
urn:ogf:network:es.net:2013::star-cr5:10_1_8:+?vlan=1779-1799
urn:ogf:network:icair.org:2013:topology:esnet?vlan=1779-1799
urn:ogf:network:icair.org:2013:topology:nl-cern1?vlan=3985
urn:ogf:network:canarie.ca:2017:topology:CHCG1?vlan=3985
urn:ogf:network:netherlight.net:2013:production7:cern-1?vlan=3989
     */
    List<Route> expResult = new ArrayList<>();
    Route route = new Route();
    StpTypeBundle bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:+?vlan=1000"), DirectionalityType.BIDIRECTIONAL);
    StpTypeBundle bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:es.net:2013::star-cr5:10_1_8:+?vlan=1779-1799"), DirectionalityType.BIDIRECTIONAL);
    route.setBundleA(bundleA);
    route.setBundleZ(bundleZ);
    route.addInternalStp("urn:ogf:network:es.net:2013::internal1");
    route.addInternalStp("urn:ogf:network:es.net:2013::internal2");
    expResult.add(route);

    route = new Route();
    bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:icair.org:2013:topology:esnet?vlan=1779-1799"), DirectionalityType.BIDIRECTIONAL);
    bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:icair.org:2013:topology:nl-cern1?vlan=3985"), DirectionalityType.BIDIRECTIONAL);
    route.setBundleA(bundleA);
    route.setBundleZ(bundleZ);
    expResult.add(route);

    route = new Route();
    bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:canarie.ca:2017:topology:CHCG1?vlan=3985"), DirectionalityType.BIDIRECTIONAL);
    bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:cern-1?vlan=3989"), DirectionalityType.BIDIRECTIONAL);
    route.setBundleA(bundleA);
    route.setBundleZ(bundleZ);
    expResult.add(route);

    List<Route> result = instance.getRoutes();
    assertEquals(expResult, result);
  }

  @Test
  public void testGeneralGetRoutes() throws Exception {
    System.out.println("getRoutes");
    SimpleStp srcStpId = new SimpleStp("urn:ogf:network:snvaca.pacificwave.net:2016:topology:irnc-dtn01.snvaca?vlan=3985");
    SimpleStp dstStpId = new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:cern-1?vlan=3985");
    net.es.nsi.pce.jaxb.path.ObjectFactory pathFactory = new net.es.nsi.pce.jaxb.path.ObjectFactory();
    StpListType ero = pathFactory.createStpListType();

    // Set internal STP for first domain.
    OrderedStpType internal0 = pathFactory.createOrderedStpType();
    internal0.setOrder(0);
    internal0.setStp("urn:ogf:network:canarie.ca:2017:topology:ANA1");
    ero.getOrderedSTP().add(internal0);

    RouteObject instance = new RouteObject(topology, srcStpId, dstStpId, DirectionalityType.BIDIRECTIONAL, Optional.of(ero));
    for (Route route : instance.getRoutes()) {
      System.out.println(route.getBundleA().getSimpleStp());
      for (OrderedStpType stp : route.getInternalStp()) {
        System.out.println("internal: " + stp.getOrder() + ", stp=" + stp.getStp());
      }
      System.out.println(route.getBundleZ().getSimpleStp());
    }

    /* Expected results:
urn:ogf:network:snvaca.pacificwave.net:2016:topology:irnc-dtn01.snvaca?vlan=3985
urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=2000-2010,2012-2019,2021-2037,2039-2100,3985-3989
urn:ogf:network:netherlight.net:2013:production7:ana-1?vlan=2000-2010,2012-2019,2021-2037,2039-2100,3985-3989
urn:ogf:network:netherlight.net:2013:production7:cern-1?vlan=3985
    */

    List<Route> expResult = new ArrayList<>();
    Route route = new Route();
    StpTypeBundle bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:snvaca.pacificwave.net:2016:topology:irnc-dtn01.snvaca?vlan=3985"), DirectionalityType.BIDIRECTIONAL);
    StpTypeBundle bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=2000-2010,2012-2019,2021-2037,2039-2100,3985-3989"), DirectionalityType.BIDIRECTIONAL);
    route.setBundleA(bundleA);
    route.setBundleZ(bundleZ);
    expResult.add(route);

    route = new Route();
    bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:ana-1?vlan=2000-2010,2012-2019,2021-2037,2039-2100,3985-3989"), DirectionalityType.BIDIRECTIONAL);
    bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:netherlight.net:2013:production7:cern-1?vlan=3985"), DirectionalityType.BIDIRECTIONAL);
    route.setBundleA(bundleA);
    route.setBundleZ(bundleZ);
    expResult.add(route);

    List<Route> result = instance.getRoutes();
    assertEquals(expResult, result);
  }

  @Test
  public void testInternalRoutes() throws Exception {
    System.out.println("getRoutes");
    SimpleStp srcStpId = new SimpleStp("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1782");
    SimpleStp dstStpId = new SimpleStp("urn:ogf:network:surfnet.nl:1990:netherlight7:2c:39:c1:38:e0:00-7-1?vlan=1782");
    net.es.nsi.pce.jaxb.path.ObjectFactory pathFactory = new net.es.nsi.pce.jaxb.path.ObjectFactory();
    StpListType ero = pathFactory.createStpListType();

    // Set internal STP for first domain.
    OrderedStpType internal0 = pathFactory.createOrderedStpType();
    internal0.setOrder(0);
    internal0.setStp("urn:ogf:network:kddilabs.jp:2013:topology:internalSTP0");
    ero.getOrderedSTP().add(internal0);

    // Set internal STP for destination domain.
    OrderedStpType internal1 = pathFactory.createOrderedStpType();
    internal1.setOrder(1);
    internal1.setStp("urn:ogf:network:surfnet.nl:1990:netherlight7:internalSTP1");
    ero.getOrderedSTP().add(internal1);


    RouteObject instance = new RouteObject(topology, srcStpId, dstStpId, DirectionalityType.BIDIRECTIONAL, Optional.of(ero));
    for (Route route : instance.getRoutes()) {
      System.out.println(route.getBundleA().getSimpleStp());
      for (OrderedStpType stp : route.getInternalStp()) {
        System.out.println("internal: " + stp.getOrder() + ", stp=" + stp.getStp());
      }
      System.out.println(route.getBundleZ().getSimpleStp());
    }

    List<Route> expResult = new ArrayList<>();
    Route route = new Route();
    StpTypeBundle bundleA = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:kddilabs.jp:2013:topology:bi-ps?vlan=1782"), DirectionalityType.BIDIRECTIONAL);
    StpTypeBundle bundleZ = new StpTypeBundle(topology, new SimpleStp("urn:ogf:network:surfnet.nl:1990:netherlight7:2c:39:c1:38:e0:00-7-1?vlan=1782"), DirectionalityType.BIDIRECTIONAL);
    route.setBundleA(bundleA);
    route.setBundleZ(bundleZ);
    route.addInternalStp("urn:ogf:network:kddilabs.jp:2013:topology:internalSTP0");
    route.addInternalStp("urn:ogf:network:surfnet.nl:1990:netherlight7:internalSTP1");
    expResult.add(route);

    List<Route> result = instance.getRoutes();
    assertEquals(expResult, result);
  }
}
