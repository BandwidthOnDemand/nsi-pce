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
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.path.jaxb.FindPathErrorType;
import net.es.nsi.pce.path.jaxb.FindPathRequestType;
import net.es.nsi.pce.path.jaxb.FindPathResponseType;
import net.es.nsi.pce.path.jaxb.FindPathStatusType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.jaxb.ReplyToType;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.path.jaxb.TypeValueType;
import net.es.nsi.pce.path.services.EthernetTypes;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class Main {
    private final static HttpConfig testServer = new HttpConfig() {
        { setUrl("http://localhost:9800/"); setPackageName("net.es.nsi.pce.client"); }
    };

    private final static String callbackURL = testServer.getUrl() + "aggregator/path";

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
        req.setAlgorithm(FindPathAlgorithmType.TREE);

        // Reservation start time is 2 minutes from now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        req.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));

        // Reservation end time is 12 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 12);
        req.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime));

        req.setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");

        // We want an EVTS service for this test.
        P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);

        // Add the STP identifiers.
        p2ps.setSourceSTP("urn:ogf:network:uvalight.net:2013:ps?vlan=1784");
        p2ps.setDestSTP("urn:ogf:network:jgn-x.jp:2013:bi-ps?vlan=1784");

        // Add MTU as an additional parameter.
        TypeValueType mtu = factory.createTypeValueType();
        mtu.setType(EthernetTypes.MTU);
        mtu.setValue("9000");
        p2ps.getParameter().add(mtu);
        req.getAny().add(factory.createP2Ps(p2ps));

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        WebTarget webTarget = client.target("http://localhost:8400/paths/find");
        response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}));
        System.out.println("Post result " + response.getStatus());

        FindPathResponseType findPathResponse = TestServer.INSTANCE.getFindPathResponse();
        int count = 0;
        while(findPathResponse == null && count < 30) {
            count++;
            Thread.sleep(1000);
        }

        if (findPathResponse != null) {
            System.out.println("FindPath Result: " + findPathResponse.getStatus().value());
            if (findPathResponse.getStatus() == FindPathStatusType.FAILED) {
                FindPathErrorType error = findPathResponse.getFindPathError();
                StringBuilder message = new StringBuilder(error.getCode());
                message.append(", ");
                message.append(error.getLabel());
                message.append(", ");
                message.append(error.getDescription());
                if (error.getVariable() != null) {
                    message.append(", ");
                    message.append(error.getVariable().getNamespace());
                    message.append(", ");
                    message.append(error.getVariable().getType());
                    if (error.getVariable().getValue() != null) {
                        message.append(", ");
                        message.append(error.getVariable().getValue());
                    }
                }
                System.out.println(message.toString());
            }
        }
        else {
            System.err.println("Failed to get result");
        }
    }
}
