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
import javax.xml.datatype.DatatypeConfigurationException;
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

/**
 * Test the FindPath interface parameter handling.
 * 
 * @author hacksaw
 */
public class FindPathParameterTest extends JerseyTest {

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
    public void testErroredXmlRequests() throws Exception {
        missingFindElementRequest(MediaType.APPLICATION_XML);
        missingCorrelationIdRequest(MediaType.APPLICATION_XML);
        missingReplyToRequest(MediaType.APPLICATION_XML);
        missingReplyToUrlRequest(MediaType.APPLICATION_XML);
        missingReplyToMediaTypeRequest(MediaType.APPLICATION_XML);
        missingAlgorithmRequest(MediaType.APPLICATION_XML);
    }
    
    @Test
    public void testErroredJsonRequests() throws Exception {
        missingFindElementRequest(MediaType.APPLICATION_JSON);
        missingCorrelationIdRequest(MediaType.APPLICATION_JSON);
        missingReplyToRequest(MediaType.APPLICATION_JSON);
        missingReplyToUrlRequest(MediaType.APPLICATION_JSON);
        missingReplyToMediaTypeRequest(MediaType.APPLICATION_JSON);
        missingAlgorithmRequest(MediaType.APPLICATION_JSON);
    }
    
    public void missingFindElementRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = null;
        
        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
        
    public void missingCorrelationIdRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);
        
        // Send invalid correlationId.
        req.setCorrelationId(null);
        
        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    public void missingReplyToRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);
        
        // Send invalid replyTo.
        req.setReplyTo(null);
        
        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    public void missingReplyToUrlRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);
        
        // Send invalid correlationId.
        req.getReplyTo().setUrl(null);
        
        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    public void missingReplyToMediaTypeRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);
        
        // Send empty mediaType which should default to specified accepted.
        req.getReplyTo().setMediaType(null);
        
        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        
        // Send invalid mediaType which should fail.
        req.getReplyTo().setMediaType("application/atom+xml");
        
        jaxbRequest = factory.createFindPathRequest(req);
        
        response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
    }

    public void missingAlgorithmRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target().path("paths/find");
        
        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);
        
        // Send empty algorithm.  Should be accepted for processing with default.
        req.setAlgorithm(null);
        
        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);
        
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }
        
    public FindPathRequestType getSuccessfulPathRequest(String mediaType) throws DatatypeConfigurationException {
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
        srcStp.setLocalId("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1");
        srcStp.setNetworkId("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
        evts.setSourceSTP(srcStp);
        evts.setSourceVLAN(1784);
        
        // Format the destination STP.
        StpType destStp = new StpType();
        destStp.setLocalId("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1");
        destStp.setNetworkId("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
        evts.setDestSTP(destStp);
        evts.setDestVLAN(1784);

        req.getAny().add(factory.createEvts(evts));
        
        return req;
    }
}
