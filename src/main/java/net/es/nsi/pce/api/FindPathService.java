package net.es.nsi.pce.api;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.EthernetBaseType;
import net.es.nsi.pce.api.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.api.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.api.jaxb.FindPathRequestType;
import net.es.nsi.pce.api.jaxb.FindPathResponseType;
import net.es.nsi.pce.api.jaxb.FindPathStatusType;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.ReplyToType;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.RestServer;
import net.es.nsi.pce.jersey.Utilities;
import net.es.nsi.pce.pf.Algorithms;
import net.es.nsi.pce.pf.PathfinderCore;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.services.Service;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            return RestServer.getBadRequestError("findPathRequest");
        }
        
        // Log the incoming request.
        if (log.isDebugEnabled()) {
            log.debug("findPath: " + XmlUtilities.jaxbToString(FindPathRequestType.class, request));
        }
        
        // We need a correlationId to send back a response.        
        String correlationId = request.getCorrelationId();
        if (correlationId == null || correlationId.isEmpty()) {
            log.error("findPath: invalid correlationId element received.");
            return RestServer.getBadRequestError("correlationId");
        }
        
        // ReplyTo is needed to send back a path response.
        ReplyToType replyTo = request.getReplyTo();
        if (replyTo == null ||
                replyTo.getUrl() == null || replyTo.getUrl().isEmpty()) {
            log.error("findPath: invalid replyTo element received.");
            return RestServer.getBadRequestError("replyTo");            
        }
        
        String mediaType = replyTo.getMediaType();
        if (mediaType == null || mediaType.isEmpty()) {
            // If MediaType is not provided then use client ACCEPT field.
            mediaType = accept;
            replyTo.setMediaType(accept);
        }
        if (!Utilities.validMediaType(mediaType)) {
            log.error("findPath: Unsupported mediaType element received.");
            return RestServer.getBadRequestError("mediaType");
        }

        // We need to have a valid algorithm to start processing.
        FindPathAlgorithmType algorithm = request.getAlgorithm();
        if (algorithm == null) {
            algorithm = FindPathAlgorithmType.CHAIN;     
        }
        else if (!Algorithms.contains(algorithm)) {
            log.error("findPath: Unsupported algorithm element received.");
            return RestServer.getBadRequestError("algorithm");
        }
        
        // Deterine if the specified serviceType is supported.
        String serviceType = request.getServiceType();
        List<Service> services = Service.getServiceByType(serviceType);
        if (services.isEmpty()) {
            log.error("findPath: Unsupported serviceType element received.");
            return RestServer.getBadRequestError("serviceType");         
        }
        
        // TODO: Place the Path Finding and FindPathResponse building code
        // in a separate model and route via Scala.
        
        // Build the Path finding response.
        FindPathResponseType resp = new FindPathResponseType();
        resp.setCorrelationId(request.getCorrelationId());
        resp.setStatus(FindPathStatusType.SUCCESS);
        
        // Invoke the path finder on associated service elements.
        PathfinderCore pathfinderCore = new PathfinderCore();
        
        // Inspect each element stored as an ANY to see if it is associated
        // with our serviceType.
        for (Object obj : request.getAny()) {
           if (obj instanceof javax.xml.bind.JAXBElement) {
                JAXBElement jaxb = (JAXBElement) obj;
                
                // We need to pull out those service elements associated with
                // the specified serviceType.
                Service inService = Service.getService(jaxb.getName().toString());
                
                // Is this service element in the list of service elements
                // associated with the serviceType?
                if (services.contains(inService)) {
                    // Invoke the service specific path finder.
                    if (inService.equals(Service.EVTS)) {
                        EthernetVlanType evts = (EthernetVlanType) jaxb.getValue();
                        try {
                            pathfinderCore.findPath(serviceType, evts, algorithm, resp.getPath());
                        }
                        catch (Exception ex) {
                            log.error("findPath: findPath EVTS failed", ex);
                            resp.setStatus(FindPathStatusType.FAILED);
                            resp.setMessage(ex.getMessage());
                        }
                    }
                    else if (inService.equals(Service.ETS)) {
                        EthernetBaseType ets = (EthernetBaseType) jaxb.getValue();
                        try {
                            pathfinderCore.findPath(serviceType, ets, algorithm, resp.getPath());
                        }
                        catch (Exception ex) {
                            resp.setStatus(FindPathStatusType.FAILED);
                            resp.setMessage(ex.getMessage());
                        }
                    }
                    else if (inService.equals(Service.P2PS)) {
                        P2PServiceBaseType p2ps = (P2PServiceBaseType) jaxb.getValue();
                        try {
                            pathfinderCore.findPath(serviceType, p2ps, algorithm, resp.getPath());
                        }
                        catch (Exception ex) {
                            resp.setStatus(FindPathStatusType.FAILED);
                            resp.setMessage(ex.getMessage());
                        }
                    }
                }
            }
        }
        
       // Now sent a response back for fun.
       JAXBElement<FindPathResponseType> jaxbRequest = factory.createFindPathResponse(resp);
        
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(replyTo.getUrl());
        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathResponseType>>(jaxbRequest) {}, mediaType));
        
        if (log.isDebugEnabled()) {
            log.debug("FindPathService: sent response " + resp.getStatus().name() + " to client " + replyTo.getUrl() + ", result = " + response.getStatusInfo().getReasonPhrase());
        }
        
        return Response.accepted().build();
    }
}