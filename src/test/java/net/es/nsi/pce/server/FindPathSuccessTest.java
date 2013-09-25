package net.es.nsi.pce.server;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import net.es.nsi.pce.jersey.RestServer;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.api.jaxb.FindPathRequestType;
import net.es.nsi.pce.api.jaxb.ReplyToType;
import net.es.nsi.pce.api.jaxb.StpType;
import net.es.nsi.pce.jersey.RestClient;

import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.client.TestServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class FindPathSuccessTest extends JerseyTest {

    private final static HttpConfig testServer = new HttpConfig() {
        { url = "http://localhost:9800/"; packageName = "net.es.nsi.pce.client"; }
    };
    
    private final static String callbackURL = testServer.url + "aggregator/path";
    
    private final static ObjectFactory factory = new ObjectFactory();
    
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        
        // Configure the local test client callback server.
        TestServer.INSTANCE.start(testServer);
        
        // Configure test instance of PCE server.
        try {
            ConfigurationManager.INSTANCE.initialize("src/test/resources/config/");
        } catch (Exception ex) {
            System.err.println("configure(): Could not initialize test environment." + ex.toString());
            fail("configure(): Could not initialize test environment.");
        }
        
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().packageName);
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }

    @Test
    public void testXmlFindPath() throws Exception {
            testSuccessfulPath(MediaType.APPLICATION_XML);
    }
    
    @Test
    public void testJsonFindPath() throws Exception {
            testSuccessfulPath(MediaType.APPLICATION_JSON);
    }
    
    @Test
    public void testVersionedXmlFindPath() throws Exception {
            testSuccessfulPath("application/vnd.net.es.pce.v1+xml");
    }

    @Test
    public void testVersionedJsonFindPath() throws Exception {
            testSuccessfulPath("application/vnd.net.es.pce.v1+json");
    }
        
    public void testSuccessfulPath(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());
        
        ReplyToType reply = new ReplyToType();
        reply.setUrl(callbackURL);
        reply.setMediaType(mediaType);
        req.setReplyTo(reply);
        req.setAlgorithm(FindPathAlgorithmType.CHAIN);
                
        // Reservation start time is 2 minutes from now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        req.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));
 
        // Reservation end time is 12 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 12);
        req.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime));
        
        req.setServiceType("http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE");

        // We want an EVTS service for this test.
        EthernetVlanType evts = new EthernetVlanType();
        evts.setCapacity(100L);
        evts.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        evts.setSymmetricPath(Boolean.TRUE);
        
        // Format the source STP.
        StpType srcStp = new StpType();
        srcStp.setLocalId("urn:ogf:network:uvalight.net:2013:bi-ps");
        srcStp.setNetworkId("urn:ogf:network:uvalight.net:2013:topology");
        evts.setSourceSTP(srcStp);
        evts.setSourceVLAN(1780);
        
        // Format the destination STP.
        StpType destStp = new StpType();
        destStp.setLocalId("urn:ogf:network:es.net:2013:ps:sunn:1");
        destStp.setNetworkId("urn:ogf:network:es.net:2013");
        evts.setDestSTP(destStp);
        evts.setDestVLAN(1780);

        req.getAny().add(factory.createEvts(evts));

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }
}
