/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.persistence.EntityExistsException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.discovery.jaxb.CollectionType;
import net.es.nsi.pce.discovery.jaxb.DocumentListType;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.ErrorType;
import net.es.nsi.pce.discovery.jaxb.NotificationListType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.jaxb.SubscriptionListType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.discovery.provider.DiscoveryProvider;
import net.es.nsi.pce.discovery.provider.Document;
import net.es.nsi.pce.discovery.provider.InvalidVersionException;
import net.es.nsi.pce.discovery.provider.Subscription;
import net.es.nsi.pce.schema.NsiConstants;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/discovery")
public class DiscoveryService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response ping() throws Exception {
        log.debug("ping: PING!");
        return Response.ok().build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getAll(
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        log.debug("getAll: summary=" + summary);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        // Get all the applicable documents.
        Collection<Document> documents = discoveryProvider.getDocuments(null, null, null, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType documentResults = factory.createDocumentListType();
        if (documents.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    documentResults.getDocument().add(document.getDocumentSummary());
                }
                else {
                    documentResults.getDocument().add(document.getDocument());
                }
            }
        }

        // Get the local documents.  There may be duplicates with the full
        // document list.
        Collection<Document> local;
        try {
            local = discoveryProvider.getLocalDocuments(null, null, lastDiscovered);
        }
        catch (NotFoundException nf) {
            local = new ArrayList<>();
        }
        catch (Exception ex) {
            log.error("getLocalDocuments: failed ", ex);
            throw ex;
        }

        DocumentListType localResults = factory.createDocumentListType();
        if (local.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : local) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    localResults.getDocument().add(document.getDocumentSummary());
                }
                else {
                    localResults.getDocument().add(document.getDocument());
                }
            }
        }

        Collection<Subscription> subscriptions = discoveryProvider.getSubscriptions(null, lastDiscovered);

        SubscriptionListType subscriptionsResults = factory.createSubscriptionListType();
        if (subscriptions.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Subscription subscription : subscriptions) {
                if (discovered.before(subscription.getLastModified())) {
                    discovered = subscription.getLastModified();
                }

                subscriptionsResults.getSubscription().add(subscription.getSubscription());
            }
        }

        if (documentResults.getDocument().isEmpty() &&
                localResults.getDocument().isEmpty() &&
                subscriptionsResults.getSubscription().isEmpty()) {
            return Response.notModified().build();
        }

        CollectionType all = factory.createCollectionType();
        all.setDocuments(documentResults);
        all.setLocal(localResults);
        all.setSubscriptions(subscriptionsResults);
        String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<CollectionType>>(factory.createCollection(all)){}).build();
    }

    @GET
    @Path("/documents")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocuments(
            @QueryParam("id") String id,
            @QueryParam("nsa") String nsa,
            @QueryParam("type") String type,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents = discoveryProvider.getDocuments(nsa, type, id, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }
        else {
            log.debug("getDocuments: zero results to query nsa=" + nsa + ", type=" + type + ", id=" + id + ", summary=" + summary);
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
    }

    @GET
    @Path("/documents/{nsa}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocumentsByNsa(
            @PathParam("nsa") String nsa,
            @QueryParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        try {
            documents = discoveryProvider.getDocumentsByNsa(nsa.trim(), type, id, lastDiscovered);
        }
        catch (IllegalArgumentException ex) {
            log.error("getDocumentsByNsa: illegal arument.\n", ex);
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (NotFoundException nf) {
            log.error("getDocumentsByNsa: resource not found.\n ", nf);
            ErrorType errorType = DiscoveryError.getErrorType(nf.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
    }

    @GET
    @Path("/documents/{nsa}/{type}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocumentsByNsaAndType(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        log.debug("getDocumentsByNsaAndType: " + nsa + ", " + type + ", " + id + ", " + summary + ", " + ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        try {
            documents = discoveryProvider.getDocumentsByNsaAndType(nsa.trim(), type.trim(), id, lastDiscovered);
        }
        catch (IllegalArgumentException ex) {
            // 400 bad request
            log.error("getDocumentsByNsaAndType: illegal arument", ex);
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
    }

    @POST
    @Path("/documents")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response addDocument(DocumentType request) throws Exception {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Document document;
        try {
            document = discoveryProvider.addDocument(request);
        }
        catch (EntityExistsException ee) {
            log.debug("addDocument: document already exists\n" + ee.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ee.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (IllegalArgumentException ia) {
            log.debug("addDocument: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (Exception ex) {
            log.error("addDocument: internal server error\n", ex);
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.serverError().entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.created(URI.create(document.getDocument().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
    }

    @POST
    @Path("/local")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response addLocalDocument(DocumentType document) throws Exception {

        return addDocument(document);
    }

    @GET
    @Path("/local")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocuments(
            @QueryParam("id") String id,
            @QueryParam("type") String type,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        try {
            documents = discoveryProvider.getLocalDocuments(type, id, lastDiscovered);
        }
        catch (IllegalArgumentException ex) {
            // 400 bad request
            log.debug("getDocumentsByNsa: illegal arugment\n" + ex.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
    }

    @GET
    @Path("/local/{type}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocumentsByType(
            @PathParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        try {
            documents = discoveryProvider.getLocalDocumentsByType(type.trim(), id, lastDiscovered);
        }
        catch (IllegalArgumentException ex) {
            // 400 bad request
            log.debug("getDocumentsByNsaAndType: illegal arument\n" + ex.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
    }

    @GET
    @Path("/local/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocument(
            @PathParam("type") String type,
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Document document;
        try {
            document = discoveryProvider.getLocalDocument(type, id, lastDiscovered);
        }
        catch (IllegalArgumentException ia) {
            // 400 Bad Request
            log.debug("getDocument: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (NotFoundException nf) {
            // 404 Not Found
            log.debug("getDocument: requested document resource not found\n" + nf.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(nf.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        if (document == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        JAXBElement<DocumentType> jaxb;
        if (summary) {
            jaxb = factory.createDocument(document.getDocumentSummary());
        }
        else {
            jaxb = factory.createDocument(document.getDocument());
        }

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
    }

    @GET
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Document document;
        try {
            document = discoveryProvider.getDocument(nsa, type, id, lastDiscovered);
        }
        catch (IllegalArgumentException ia) {
            // 400 Bad Request
            log.debug("getDocument: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (NotFoundException nf) {
            // 404 Not Found
            log.debug("getDocument: requested document resource not found\n" + nf.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(nf.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        if (document == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        JAXBElement<DocumentType> jaxb;
        if (summary) {
            jaxb = factory.createDocument(document.getDocumentSummary());
        }
        else {
            jaxb = factory.createDocument(document.getDocument());
        }

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
    }

    @PUT
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response updateDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id,
            DocumentType request) throws Exception {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Document document;
        try {
            document = discoveryProvider.updateDocument(nsa, type, id, request);
        }
        catch (NotFoundException ee) {
            log.debug("updateDocument: document does not exist\n" + ee.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ee.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (IllegalArgumentException ia) {
            log.debug("updateDocument: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (InvalidVersionException vr) {
            log.debug("updateDocument: invalid version\n" + vr.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(vr.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (Exception ex) {
            log.debug("updateDocument: internal server error\n" + ex.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.serverError().entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
    }

    @GET
    @Path("/subscriptions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscriptions(
            @QueryParam("requesterId") String requesterId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        log.debug("getSubscriptions: " + requesterId + ", " + ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastModified = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastModified = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Subscription> subscriptions;
        try {
            subscriptions = discoveryProvider.getSubscriptions(requesterId, lastModified);
        }
        catch (IllegalArgumentException ex) {
            // 400 bad request
            log.debug("getSubscriptions: illegal arument\n" + ex.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        Date modified = new Date(0);
        SubscriptionListType results = factory.createSubscriptionListType();
        if (subscriptions.size() > 0) {
            for (Subscription subscription : subscriptions) {
                if (modified.before(subscription.getLastModified())) {
                    modified = subscription.getLastModified();
                }

                results.getSubscription().add(subscription.getSubscription());
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<SubscriptionListType> jaxb = factory.createSubscriptions(results);
        if (results.getSubscription().size() > 0) {
            String date = DateUtils.formatDate(modified, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<SubscriptionListType>>(jaxb){}).build();
    }

    @POST
    @Path("/subscriptions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response addSubscription(
            @HeaderParam("Accept") String accept,
            SubscriptionRequestType subscriptionRequest) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Subscription subscription;
        try {
            subscription = discoveryProvider.addSubscription(subscriptionRequest, accept);
        }
        catch (IllegalArgumentException ia) {
            // 400 bad request
            log.debug("addSubscription: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (Exception ex) {
            //500 - Internal server error (Catchall)
            log.debug("addSubscription: internal server error\n" + ex.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ex.getMessage());
            return Response.serverError().entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());
        return Response.created(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
    }

    @GET
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscription(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Subscription subscription;
        try {
            subscription = discoveryProvider.getSubscription(id, lastDiscovered);
        }
        catch (IllegalArgumentException ia) {
            // 400 Bad Request
            log.debug("getSubscription: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (NotFoundException nf) {
            // 404 Not Found
            log.debug("getSubscription: requested subscription resource not found, id=" + id + "\n" + nf.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(nf.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        if (subscription == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());


        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
    }

    @PUT
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response editSubscription(
            @HeaderParam("Accept") String accept,
            @PathParam("id") String id,
            SubscriptionRequestType subscriptionRequest) throws Exception {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Subscription subscription;
        try {
            subscription = discoveryProvider.editSubscription(id, subscriptionRequest, accept);
        }
        catch (IllegalArgumentException ia) {
            // 400 bad request
            log.debug("editSubscription: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (NotFoundException nf) {
            // 404 Not Found
            log.debug("editSubscription: requested subscription resource not found, id=" + id + "\n" + nf.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(nf.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());
        return Response.ok(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
    }

    @DELETE
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response deleteSubscription(@PathParam("id") String id) throws Exception {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        try {
            discoveryProvider.deleteSubscription(id);
        }
        catch (NotFoundException ee) {
            // 404 not found
            log.debug("deleteSubscription: subscription does not exist, id=" + id + "\n" + ee.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ee.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }
        catch (IllegalArgumentException ia) {
            // 400 bad request
            log.debug("deleteSubscription: illegal arument\n" + ia.getMessage());
            ErrorType errorType = DiscoveryError.getErrorType(ia.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(errorType)){}).build();
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/notifications")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response notifications(NotificationListType notifications) throws Exception {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        if (notifications != null) {
            log.debug("notifications: provider=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId() + ", href=" + notifications.getHref() + ", discovered=" + notifications.getDiscovered());
            for (NotificationType notification : notifications.getNotification()) {
                log.debug("notifications: processing notification event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
                try {
                    discoveryProvider.processNotification(notification);
                }
                catch (Exception ex) {
                    log.error("notifications: failed to process notification for documentId=" + notification.getDocument().getId(), ex);
                }
            }
        }
        else {
            log.error("notifications: Received empty notification.");
        }

        return Response.accepted().build();
    }
}
