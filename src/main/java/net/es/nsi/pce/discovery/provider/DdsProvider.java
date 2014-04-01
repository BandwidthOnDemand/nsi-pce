/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.provider;

import net.es.nsi.pce.discovery.actors.RemoteSubscription;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityExistsException;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.discovery.actors.DocumentEvent;
import net.es.nsi.pce.discovery.actors.DocumentExpiryActor;
import net.es.nsi.pce.discovery.actors.LocalDocumentActor;
import net.es.nsi.pce.discovery.actors.NotificationRouter;
import net.es.nsi.pce.discovery.actors.RegistrationRouter;
import net.es.nsi.pce.discovery.actors.SubscriptionEvent;
import net.es.nsi.pce.discovery.api.DiscoveryError;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.FilterType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.schema.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class DdsProvider implements DiscoveryProvider {
    private static final String NOTIFICATIONS_URL = "/discovery/notifications";
    private static final String MEDIATYPE = "application/vnd.ogf.nsi.discovery.v1+xml";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private ActorSystem actorSystem;
    private ActorRef notificationRouter;
    private ActorRef registrationRouter;
    private ActorRef localDocumentActor;
    private ActorRef cacheActor;
    
    // Configuration reader.
    private ConfigurationReader configReader;
    
    // In-memory document cache.
    private DocumentCache documentCache;
    
    // In-memory subscription cache indexed by subscriptionId.
    private Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    
    // In-memory subscription cache indexed by subscriptionId.
    private Map<String, RemoteSubscription> remoteSubscriptions = new ConcurrentHashMap<>();
 
    public DdsProvider(ConfigurationReader configuration, DocumentCache documentCache) {
        this.configReader = configuration;
        this.documentCache = documentCache;
    }  

    @Override
    public void initialize() throws IllegalArgumentException, JAXBException, FileNotFoundException {
        // Initialize the AKKA actor system for the PCE and subsystems.
        log.info("DdsProvider: Initializing actor framework...");
        setActorSystem(ActorSystem.create("NSI-DISCOVERY"));
        log.info("DdsProvider: ... Actor framework initialized.");
        
        // Initialize the subscription notification actors.
        log.info("DdsProvider: Initializing notification router.");
        notificationRouter = getActorSystem().actorOf(Props.create(NotificationRouter.class, getConfigReader().getActorPool()), "discovery-notification-router");
        log.info("DdsProvider:... Notification router initialized.");
        
        // Load our local documents only if a local directory is configured.
        if (configReader.getDocuments() != null && !configReader.getDocuments().isEmpty()) {
            log.info("DdsProvider: Initializing local document repository.");
            localDocumentActor = getActorSystem().actorOf(Props.create(LocalDocumentActor.class, this), "discovery-document-watcher");
            log.info("DdsProvider:... Local document repository initialized.");
        }

        // Initialize document expiry actor.
        log.info("DdsProvider: Initializing document expiry actor.");
        localDocumentActor = getActorSystem().actorOf(Props.create(DocumentExpiryActor.class, this), "discovery-expiry-watcher");
        log.info("DdsProvider:... Document expiry actor initialized.");        
        
        // Initialize the remote registration actors.
        log.info("DdsProvider: Initializing peer registration actor...");
        setRegistrationRouter(getActorSystem().actorOf(Props.create(RegistrationRouter.class, this), "discovery-peer-registration"));        
        log.info("DdsProvider:... Peer registration actor initialized.");
    }
    
    @Override
    public String getNsaId() {
        return getConfigReader().getNsaId();
    }
    
    @Override
    public String getNotificationURL() throws MalformedURLException {
        URL url = new URL(getConfigReader().getBaseURL());
        url = new URL(url, NOTIFICATIONS_URL);
        return url.toString();
    }
    
    @Override
    public String getMediaType() {
        return MEDIATYPE;
    }
 
    @Override
    public Subscription addSubscription(SubscriptionRequestType request, String encoding) {
        // Populate a subscription object.
        Subscription subscription = new Subscription(request, encoding);
        
        // Save the subscription.
        subscriptions.put(subscription.getId(), subscription);
        // Now we need to schedule the send of the initail set of matching
        // documents in a notification to this subscription.  We delay the
        // send so that the requester has time to return and store the
        // subscription identifier.
        SubscriptionEvent se = new SubscriptionEvent();
        se.setEvent(SubscriptionEvent.Event.New);
        se.setSubscription(subscription);
        Cancellable scheduleOnce = getActorSystem().scheduler().scheduleOnce(Duration.create(10, TimeUnit.SECONDS), notificationRouter, se, getActorSystem().dispatcher(), null);
        subscription.setAction(scheduleOnce);
        
        return subscription;
    }
    
    @Override
    public Subscription deleteSubscription(String id) throws IllegalArgumentException, NotFoundException {
        if (id == null || id.isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "id");
            throw new IllegalArgumentException(error); 
        }
        
        Subscription subscription = subscriptions.remove(id);
        if (subscription == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
            throw new NotFoundException(error);              
        }
        
        if (subscription.getAction() != null) {
            subscription.getAction().cancel();
            subscription.setAction(null);
        }
        
        return subscription;
    }
    
    @Override
    public Subscription editSubscription(String id, SubscriptionRequestType request, String encoding) throws IllegalArgumentException, NotFoundException {
        // Make sure we have all needed parameters.
        if (id == null || id.isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "id");
            throw new IllegalArgumentException(error); 
        }
        
        Subscription subscription = subscriptions.get(id);
        if (subscription == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
            throw new NotFoundException(error);              
        }
        
        subscription.setEncoding(encoding);
        subscription.setLastModified(new Date());
        SubscriptionType sub = subscription.getSubscription();
        sub.setRequesterId(request.getRequesterId());
        sub.setFilter(request.getFilter());
        sub.setCallback(request.getCallback());
        sub.getAny().addAll(request.getAny());
        sub.getOtherAttributes().putAll(request.getOtherAttributes());

        SubscriptionEvent se = new SubscriptionEvent();
        se.setEvent(SubscriptionEvent.Event.Update);
        se.setSubscription(subscription);
        notificationRouter.tell(se, null);
        
        return subscription;
    }
    
    @Override
    public Subscription getSubscription(String id, Date lastModified) throws IllegalArgumentException, NotFoundException {
        if (id == null || id.isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "id");
            throw new IllegalArgumentException(error); 
        }
        
        Subscription subscription = subscriptions.get(id);
        if (subscription == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
            throw new NotFoundException(error);              
        }
        
        // Check to see if the document was modified after provided date.
        if (lastModified != null &&
                lastModified.after(subscription.getLastModified())) {
            // NULL will represent not modified.
            return null;
        }
        
        return subscription;
    }
    
    @Override
    public Collection<Subscription> getSubscriptions(String requesterId, Date lastModified) {
        
        Collection<Subscription> subs = new ArrayList<>();
        if (requesterId != null && !requesterId.isEmpty()) {
            subs = getSubscriptionByRequesterId(requesterId, subscriptions.values());
        }
        else {
            subs.addAll(subscriptions.values());
        }
        
        if (lastModified != null) {
            subs = getSubscriptionsByDate(lastModified, subs);
        }

        return subs;
    }
    
    public Collection<Subscription> getSubscriptionByRequesterId(String requesterId, Collection<Subscription> input) {
        Collection<Subscription> output = new ArrayList<>();
        for (Subscription subscription : input) {
            if (subscription.getSubscription().getRequesterId().equalsIgnoreCase(requesterId)) {
                output.add(subscription);
            }
        }
        
        return output;
    }
    
    public Collection<Subscription> getSubscriptionsByDate(Date lastModified, Collection<Subscription> input) {
        Collection<Subscription> output = new ArrayList<>();
        for (Subscription subscription : input) {
            if (subscription.getLastModified().after(lastModified)) {
                output.add(subscription);
            }
        }
        
        return output;
    }
    
    @Override
    public void processNotification(NotificationType notification) {
        log.debug("processNotification: event=" + notification.getEvent() + ", discovered=" + notification.getDiscovered());
        
        // Determine if we have already seen this document event.
        DocumentType document = notification.getDocument();
        if (document == null) {
            log.debug("processNotification: Document null.");
            return;
        }
        
        String documentId = Document.documentId(document.getNsa(), document.getType(), document.getId());
        
        Document entry = documentCache.get(documentId);
        if (entry == null) {
            // This must be the first time we have seen the document so add it
            // into our cache.
            log.debug("processNotification: new document documentId=" + documentId);
            addDocument(document);
        }
        else if (entry.getDocument().getVersion().compare(document.getVersion()) == DatatypeConstants.LESSER) {
            // We have seen the document before and this is a new version, so
            // update our cache.
            log.debug("processNotification: document update documentId=" + documentId);
            updateDocument(document.getNsa(), document.getType(), document.getId(), document);
        }
        else {
            // Otherwise we discard the notification.
            log.debug("processNotification: discarding event documentId=" + documentId);
        }
    }
    
    @Override
    public Document addDocument(DocumentType request) throws IllegalArgumentException, EntityExistsException {
        // Create and populate our internal document.
        Document document = new Document(request);
        
        // See if we already have a document under this id.
        Document get = documentCache.get(document.getId());
        if (get != null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_EXISTS, "document", document.getId());
            throw new EntityExistsException(error);            
        }

        // This is a new document so add it into the document space.
        try {
            documentCache.put(document.getId(), document);
        }
        catch (Exception ex) {
            log.error("addDocument: failed to add document to document store", ex);
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_INVALID, "document", document.getId());
            throw new EntityExistsException(error);             
        }
        
        // Route a new document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.NEW);
        de.setDocument(document);
        notificationRouter.tell(de, null);
        return document;
    }
    
    @Override
    public Document updateDocument(String nsa, String type, String id, DocumentType request) throws IllegalArgumentException, NotFoundException {
        // Create a document identifier to look up in our documet table.
        String documentId = Document.documentId(nsa, type, id);
        
        log.debug("updateDocument: documentId=" + documentId);
        
        // See if we have a document under this id.
        Document document = documentCache.get(documentId);
        if (document == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", documentId);
            throw new NotFoundException(error);            
        }

        // Validate basic fields.
        if (request.getNsa() == null || request.getNsa().isEmpty() || !request.getNsa().equalsIgnoreCase(document.getDocument().getNsa())) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
            throw new IllegalArgumentException(error); 
        }

        if (request.getType() == null || request.getType().isEmpty() || !request.getType().equalsIgnoreCase(document.getDocument().getType())) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "type");
            throw new IllegalArgumentException(error); 
        }

        if (request.getId() == null || request.getId().isEmpty() || !request.getId().equalsIgnoreCase(document.getDocument().getId())) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "id");
            throw new IllegalArgumentException(error); 
        }

        if (request.getVersion() == null || !request.getVersion().isValid()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "version");
            throw new IllegalArgumentException(error); 
        }

        if (request.getExpires() == null || !request.getExpires().isValid()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "expires");
            throw new IllegalArgumentException(error); 
        }
   
        document.setDocument(request);
        document.setLastDiscovered(new Date());

        // Route a update document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.UPDATED);
        de.setDocument(document);
        notificationRouter.tell(de, null);
        
        return document;
    }
    
    @Override
    public Document updateDocument(DocumentType request) throws IllegalArgumentException, NotFoundException {
        return updateDocument(request.getNsa(), request.getType(), request.getId(), request);
    }

    @Override
    public Collection<Document> getDocuments(String nsa, String type, String id, Date lastDiscovered) {
        // We need to search for matching documents using the supplied criteria.
        // We will do this linearly now, but we will need multiple indicies later
        // to make this faster (perhaps a database).
        
        // Seed the results.
        Collection<Document> results = documentCache.values();
        
        // This may be the most often used so filter by this first.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }

        if (nsa != null && !nsa.isEmpty()) {
            results = getDocumentsByNsa(nsa, results);
        }
        
        if (type != null && !type.isEmpty()) {
            results = getDocumentsByType(type, results);
        }
        
        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        return results;
    }
    
    @Override
    public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException {
        // Seed the results.
        Collection<Document> results = documentCache.values();

        // This is the primary search value.  Make sure it is present.
        if (nsa != null && !nsa.isEmpty()) {
            results = getDocumentsByNsa(nsa, results);
        }
        else {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
            throw new IllegalArgumentException(error);            
        }
        
        // The rest are additional filters.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }
        
        if (type != null && !type.isEmpty()) {
            results = getDocumentsByType(type, results);
        }
        
        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        return results;
    }
    
    @Override
    public Collection<Document> getDocumentsByNsaAndType(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException {
        // Seed the results.
        Collection<Document> results = documentCache.values();

        log.debug("getDocumentsByNsaAndType: " + results.size());
        
        // This is the primary search value.  Make sure it is present.
        if (nsa == null || nsa.isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
            throw new IllegalArgumentException(error);   
        }

        if (type == null || type.isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "type");
            throw new IllegalArgumentException(error);            
        }
        
        results = getDocumentsByNsa(nsa, results);
        results = getDocumentsByType(type, results);
                
        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        // The rest are additional filters.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }

        return results;
    }
    
    @Override
    public Document getDocument(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException, NotFoundException {
        String documentId = Document.documentId(nsa, type, id);
        Document document = documentCache.get(documentId);
        
        if (document == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", "nsa=" + nsa + ", type=" + type + ", id=" + id);
            throw new NotFoundException(error);
        }
        
        // Check to see if the document was modified after provided date.
        if (lastDiscovered != null &&
                lastDiscovered.after(document.getLastDiscovered())) {
            // NULL will represent not modified.
            return null;
        }
        
        return document;
    }
    
    @Override
    public Document getDocument(DocumentType document) throws IllegalArgumentException, NotFoundException {
        String documentId = Document.documentId(document);
        return documentCache.get(documentId);
    }
    
    @Override
    public Collection<Document> getLocalDocuments(String type, String id, Date lastDiscovered) throws IllegalArgumentException {
        return getDocumentsByNsa(getConfigReader().getNsaId(), type, id, lastDiscovered);
    }
    
    @Override
    public Collection<Document> getLocalDocumentsByType(String type, String id, Date lastDiscovered) throws IllegalArgumentException {
        return getDocumentsByNsaAndType(getConfigReader().getNsaId(), type, id, lastDiscovered);
    }
    
    @Override
    public Document getLocalDocument(String type, String id, Date lastDiscovered) throws IllegalArgumentException, NotFoundException {
        return getDocument(getConfigReader().getNsaId(), type, id, lastDiscovered);
    }
    
    public Collection<Document> getDocumentsByDate(Date lastDiscovered, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        for (Document document : input) {
            if (document.getLastDiscovered().after(lastDiscovered)) {
                output.add(document);
            }
        }
        
        return output;
    }
    
    public Collection<Document> getDocumentsByNsa(String nsa, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        for (Document document : input) {
            if (document.getDocument().getNsa().equalsIgnoreCase(nsa)) {
                output.add(document);
            }
        }
        
        return output;
    }
    
    public Collection<Document> getDocumentsByType(String type, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        for (Document document : input) {
            if (document.getDocument().getType().equalsIgnoreCase(type)) {
                output.add(document);
            }
        }
        
        return output;
    }
    
    public Collection<Document> getDocumentsById(String id, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        for (Document document : input) {
            if (document.getDocument().getId().equalsIgnoreCase(id)) {
                output.add(document);
            }
        }
        
        return output;
    }
    
    @Override
    public Collection<Document> getDocuments(FilterType filter) {
        // TODO: Match everything for demo.  Need to fix later.
        return documentCache.values();
    }
    
    @Override
    public Collection<Subscription> getSubscriptions(DocumentEvent event) {
        // TODO: Match everything for demo.  Need to fix later.
        return subscriptions.values();
    }

    @Override
    public void shutdown() {
        getActorSystem().shutdown();
    }

    /**
     * @return the configReader
     */
    public ConfigurationReader getConfigReader() {
        return configReader;
    }

    /**
     * @param configReader the configReader to set
     */
    public void setConfigReader(ConfigurationReader configReader) {
        this.configReader = configReader;
    }

    /**
     * @return the actorSystem
     */
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    /**
     * @param actorSystem the actorSystem to set
     */
    public void setActorSystem(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }
    
    public RemoteSubscription getRemoteSubscription(String url) {
        return remoteSubscriptions.get(url);
    }
    
    public RemoteSubscription addRemoteSubscription(RemoteSubscription subscription) {
        return remoteSubscriptions.put(subscription.getDdsURL(), subscription);
    }
    
    public RemoteSubscription removeRemoteSubscription(String url) {
        return remoteSubscriptions.remove(url);
    }
    
    public Collection<RemoteSubscription> remoteSubscriptionValues() {
        return remoteSubscriptions.values();
    }
    
    public Set<String> remoteSubscriptionKeys() {
        return remoteSubscriptions.keySet();
    }

    /**
     * @return the registrationRouter
     */
    public ActorRef getRegistrationRouter() {
        return registrationRouter;
    }

    /**
     * @param registrationRouter the registrationRouter to set
     */
    public void setRegistrationRouter(ActorRef registrationRouter) {
        this.registrationRouter = registrationRouter;
    }

    @Override
    public void loadDocuments(String path) {
        Collection<String> xmlFilenames = XmlUtilities.getXmlFilenames(path);
        for (String filename : xmlFilenames) {
            DocumentType document;
            try {
                document = DiscoveryParser.getInstance().readDocument(filename);
                if (document == null) {
                    log.error("loadDocuments: Loaded empty document from " + filename);
                    continue;
                }
            }
            catch (JAXBException | FileNotFoundException ex) {
                log.error("loadDocuments: Failed to load file " + filename, ex);
                continue;
            }

            // We need to determine if this document is still valid
            // before proceding.
            XMLGregorianCalendar expires = document.getExpires();
            if (expires != null) {
                Date expiresTime = expires.toGregorianCalendar().getTime();

                // We take the current time and add the expiry buffer.
                Date now = new Date();
                now.setTime(now.getTime() + this.getConfigReader().getExpiryInterval() * 1000);
                if (expiresTime.before(now)) {
                    // This document is old and no longer valid.
                    log.error("loadDocuments: Loaded document has expired " + filename + ", expires=" + expires.toGregorianCalendar().getTime().toString());
                    continue;
                }
            }

            // See if we have seen this document before.
            Document existingDocument = this.getDocument(document);
            if (existingDocument == null) {
                // We have not seen this document so add it.
                try {
                    this.addDocument(document);
                }
                catch (IllegalArgumentException| EntityExistsException ex) {
                    log.error("loadDocuments: Could not add document " + filename, ex);
                    continue;                                
                }

                log.debug("loadDocuments: added document " + filename);
            }
            else {
                // We need to check if this is a new version of the document.
                XMLGregorianCalendar existingVersion = existingDocument.getDocument().getVersion();
                if (existingVersion != null &&
                        existingVersion.compare(document.getVersion()) == DatatypeConstants.LESSER) {
                    // The existing version is older so add the new one.
                    try {
                        this.updateDocument(document);
                    }
                    catch (IllegalArgumentException | NotFoundException ex) {
                        log.error("loadDocuments: Could not update document " + filename, ex);
                        continue;                                
                    }                                
                }

                log.debug("loadDocuments: updated document " + filename);
            }
        }
    }

    @Override
    public void expireDocuments() {
        documentCache.expire();
    }
}
