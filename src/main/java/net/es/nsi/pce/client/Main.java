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
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.FindPathAlgorithmType;
import net.es.nsi.pce.jaxb.path.FindPathErrorType;
import net.es.nsi.pce.jaxb.path.FindPathRequestType;
import net.es.nsi.pce.jaxb.path.FindPathResponseType;
import net.es.nsi.pce.jaxb.path.FindPathStatusType;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.path.ReplyToType;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jaxb.path.ConstraintListType;
import net.es.nsi.pce.jaxb.path.ConstraintType;
import net.es.nsi.pce.jaxb.path.OrderedStpType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.jaxb.path.TypeValueType;
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

        ConstraintType constraint1 = new ConstraintType();
        constraint1.setType("http://schemas.ogf.org/nsi/2013/12/services/types#stpId");
        constraint1.setValue("urn:ogf:network:manlan.internet2.edu:2013:es?vlan=3400");

        ConstraintType constraint2 = new ConstraintType();
        constraint2.setType("http://schemas.ogf.org/nsi/2013/12/services/types#stpId");
        constraint2.setValue("urn:ogf:network:manlan.internet2.edu:2013:es?vlan=3488-3499");

        ConstraintListType list = new ConstraintListType();
        list.getExclude().add(constraint1);
        list.getExclude().add(constraint2);
        req.setConstraints(list);

        // We want an EVTS service for this test.
        P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);

        // Add the STP identifiers.
        p2ps.setSourceSTP("urn:ogf:network:cipo.rnp.br:2014:rjo-1?vlan=1801");
        p2ps.setDestSTP("urn:ogf:network:ampath.net:2013:topology:starlight?vlan=1799");

        // Set ERO.
        StpListType ero = factory.createStpListType();
        OrderedStpType stp0 = factory.createOrderedStpType();
        stp0.setOrder(0);
        stp0.setStp("urn:ogf:network:manlan.internet2.edu:2013:geant-lag");
        ero.getOrderedSTP().add(stp0);

        OrderedStpType stp1 = factory.createOrderedStpType();
        stp1.setOrder(1);
        stp1.setStp("urn:ogf:network:geant.net:2013:topology:bi-geant-surfnet1");
        ero.getOrderedSTP().add(stp1);

        p2ps.setEro(ero);

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
