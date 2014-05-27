package net.es.nsi.pce.path.api;

import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.path.jaxb.FindPathRequestType;
import net.es.nsi.pce.path.jaxb.FindPathResponseType;
import net.es.nsi.pce.path.jaxb.FindPathStatusType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.path.jaxb.ReplyToType;
import net.es.nsi.pce.path.jaxb.ResolvedPathType;
import net.es.nsi.pce.jersey.RestClient;
import static net.es.nsi.pce.jersey.RestClient.configureClient;
import net.es.nsi.pce.jersey.RestServer;
import net.es.nsi.pce.jersey.Utilities;
import net.es.nsi.pce.path.jaxb.TraceType;
import net.es.nsi.pce.pf.Algorithms;
import net.es.nsi.pce.pf.PathfinderCore;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.path.services.Point2Point;
import net.es.nsi.pce.path.services.Service;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

/**
 * Find Path REST service will perform path computation on provided NSI service
 * request.  Results to path computation are returned to the client using an
 * HTTP POST operation to the provided client endpoint.
 *
 * @author hacksaw
 */
@Path("/paths")
public class FindPathService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Factory for FindPath request and response messages.
    private final static ObjectFactory factory = new ObjectFactory();

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response ping() throws Exception {
        log.debug("ping: PING!");
        return Response.ok().build();
    }

    /**
     * JAX-RS annotated method exposing the FindPath operation.  This method
     * supports both JSON and XML encodings.
     *
     * The following status codes will be returned:
     *  - ACCEPTED (202) if the request has been accepted for processing.
     *  - BAD REQUEST (400) if the request is malformed.
     *  - SERVER ERROR (500) if there is an unexpected exception.
     *
     * @param accept The MediaType the client will accept in a response.  Used if MediaType is not specified in the replyTo field.
     * @param request The client find path request.
     * @return HTTP Response object identifying a status of the request.
     *
     */
    @POST
    @Path("/find")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response findPath(@HeaderParam("Accept") String accept, FindPathRequestType request) throws Exception {
        // Verify we have a request body.
        if (request == null) {
            log.error("findPath: empty request received.");
            return RestServer.getBadRequestError("findPathRequest", "request is empty");
        }

        // Log the incoming request.
        if (log.isDebugEnabled()) {
            log.debug("*** findPath: \n" + XmlUtilities.jaxbToString(FindPathRequestType.class, request));
        }

        // We need a correlationId to send back a response.
        String correlationId = request.getCorrelationId();
        if (correlationId == null || correlationId.isEmpty()) {
            log.error("findPath: invalid correlationId element received.");
            return RestServer.getBadRequestError("findPathRequest", "correlationId");
        }

        // ReplyTo is needed to send back a path response.
        ReplyToType replyTo = request.getReplyTo();
        if (replyTo == null ||
                replyTo.getUrl() == null || replyTo.getUrl().isEmpty()) {
            log.error("findPath: invalid replyTo element received.");
            return RestServer.getBadRequestError("findPathRequest", "replyTo");
        }

        String mediaType = replyTo.getMediaType();
        if (mediaType == null || mediaType.isEmpty()) {
            // If MediaType is not provided then use client ACCEPT field.
            mediaType = accept;
            replyTo.setMediaType(accept);
        }
        if (!Utilities.validMediaType(mediaType)) {
            log.error("findPath: Unsupported mediaType element received.");
            return RestServer.getBadRequestError("findPathRequest", "mediaType");
        }

        // We need to have a valid algorithm to start processing.
        FindPathAlgorithmType algorithm = request.getAlgorithm();
        if (algorithm == null) {
            algorithm = FindPathAlgorithmType.TREE;
        }
        else if (!Algorithms.contains(algorithm)) {
            log.error("findPath: Unsupported algorithm element received.");
            return RestServer.getBadRequestError("findPathRequest", "algorithm");
        }

        // Deterine if the specified serviceType is supported.
        String serviceType = request.getServiceType();
        List<Service> services = Service.getServiceByType(serviceType);
        if (services.isEmpty()) {
            log.error("findPath: Unsupported serviceType element received.");
            return RestServer.getBadRequestError("findPathRequest", "serviceType");
        }

        //
        // TODO: Place the Path Finding and FindPathResponse building code
        // in a separate model and route via Scala.

        // Build the Path finding response.
        FindPathResponseType resp = new FindPathResponseType();
        resp.setCorrelationId(request.getCorrelationId());
        resp.setStatus(FindPathStatusType.SUCCESS);

        // Invoke the path finder on associated service elements.
        PathfinderCore pathfinderCore;
        try {
            pathfinderCore = (PathfinderCore) SpringApplicationContext.getBean("pathfinderCore");
        }
        catch (BeansException be) {
            log.error("findPath: Cannot find pathfinderCore bean.", be);
            return RestServer.getInternalServerError("findPathRequest", "pathfinderCore");
        }

        // Inspect each element stored as an ANY to see if it is associated
        // with our serviceType.  At the moment we are invoking these in
        // sequence and not sending all elements pf the serviceType into the
        // PCE at the same time.
        for (Object obj : request.getAny()) {
           if (obj instanceof javax.xml.bind.JAXBElement) {
                JAXBElement<?> jaxb = (JAXBElement<?>) obj;

                // We need to pull out those service elements associated with
                // the specified serviceType.
                Service inService = Service.getService(jaxb.getName().toString());

                // Is this service element in the list of service elements
                // associated with the serviceType?
                if (services.contains(inService)) {
                    // Invoke the service specific path finder.
                    if (inService.equals(Service.P2PS) && jaxb.getValue() instanceof P2PServiceBaseType) {
                        P2PServiceBaseType p2ps = (P2PServiceBaseType) jaxb.getValue();
                        Point2Point p2p = new Point2Point();

                        // Convert the findPath API to constraints.
                        Set<Constraint> contraints = new HashSet<>();
                        contraints.addAll(PCEConstraints.getConstraints(request.getStartTime(), request.getEndTime(), serviceType, request.getConstraints()));

                        // Now the service specific constraints.
                        Set<Constraint> addConstraints = p2p.addConstraints(p2ps);
                        contraints.addAll(addConstraints);
                        try {
                            net.es.nsi.pce.pf.api.Path path = pathfinderCore.findPath(algorithm, contraints, convertTrace(request.getTrace()));
                            List<ResolvedPathType> resolved = p2p.resolvePath(path);
                            resp.getPath().addAll(resolved);
                        }
                        catch (Exception ex) {
                            log.error("findPath: findPath P2PS failed", ex);
                            resp.setStatus(FindPathStatusType.FAILED);
                            resp.setFindPathError(NsiError.getFindPathError(ex.getMessage()));
                            break;
                        }
                    }
                }
                else {
                    log.error("Found element " + jaxb.getName().toString() + " that is not part of service definition " + request.getServiceType());
                    return RestServer.getBadRequestError("findPathRequest", jaxb.getName().toString() + " not in " + request.getServiceType());
                }
            }
        }

       // Now sent a response back for fun.
       JAXBElement<FindPathResponseType> jaxbRequest = factory.createFindPathResponse(resp);

        // Client client = RestClient.getInstance().get();

        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);

        Response response;
        WebTarget webTarget = client.target(replyTo.getUrl());
        try {
            response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathResponseType>>(jaxbRequest) {}, mediaType));

            if (log.isDebugEnabled()) {
                log.debug("FindPathService: sent response " + resp.getStatus().name() + " to client " + replyTo.getUrl() + ", result = " + response.getStatusInfo().getReasonPhrase());
            }
        }
        catch (WebApplicationException wex) {
            log.error("Send of path results failed", wex);
            client.close();
            return RestServer.getBadRequestError("findPathRequest", "Send of path results to endpoint " + replyTo.getUrl() + "failed with status " + wex.getResponse().getStatus());
        }
        catch (Exception ex) {
            log.error("Send of path results failed", ex);
            client.close();
            return RestServer.getBadRequestError("findPathRequest", "Send of path results to endpoint " + replyTo.getUrl() + "failed with " + ex.getLocalizedMessage());
        }

        response.close();
        client.close();
        return Response.accepted().build();
    }

    public List<String> convertTrace(List<TraceType> trace) {
        Comparator<TraceType> traceComp = new Comparator<TraceType>() {
            @Override
            public int compare(TraceType b1, TraceType b2) {
                if (b1.getIndex() < b2.getIndex()) {
                    return -1;
                }
                else if (b1.getIndex() == b2.getIndex()) {
                    return 0;
                }
                else {
                    return 1;
                }
            }
        };

        List<TraceType> sortedList = Ordering.from(traceComp).sortedCopy(trace);

        List<String> list = new ArrayList<>();
        for (TraceType item : sortedList) {
            list.add(item.getValue());
        }
        return list;
    }
}