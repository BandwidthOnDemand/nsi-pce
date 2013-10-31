/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.client;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.api.jaxb.FindPathRequestType;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.ReplyToType;
import net.es.nsi.pce.api.jaxb.StpType;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.jersey.RestClient;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class Main {
    private final static HttpConfig testServer = new HttpConfig() {
        { url = "http://localhost:9800/"; packageName = "net.es.nsi.pce.client"; }
    };
    
    private final static String callbackURL = testServer.url + "aggregator/path";
    
    private final static ObjectFactory factory = new ObjectFactory();
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget webGet = client.target("http://localhost:8400/paths");
        Response response = webGet.request(MediaType.APPLICATION_JSON).get();
        
        System.out.println("Get result " + response.getStatus());
       
        // Configure the local test client callback server.
        TestServer.INSTANCE.start(testServer);
        
        // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());
        
        ReplyToType reply = new ReplyToType();
        reply.setUrl(callbackURL);
        reply.setMediaType(MediaType.APPLICATION_JSON);
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
        srcStp.setLocalId("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:282");
        srcStp.setNetworkId("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed");
        evts.setSourceSTP(srcStp);
        evts.setSourceVLAN(1784);
        
        // Format the destination STP.
        StpType destStp = new StpType();
        destStp.setLocalId("urn:ogf:network:singaren.net:2013:videohost");
        destStp.setNetworkId("urn:ogf:network:singaren.net:2013");
        evts.setDestSTP(destStp);
        evts.setDestVLAN(1784);

        req.getAny().add(factory.createEvts(evts));

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        WebTarget webTarget = client.target("http://localhost:8400/paths/find");
        response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}));
        System.out.println("Post result " + response.getStatus());
    }
}
