/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.provider;

import akka.actor.Cancellable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityExistsException;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.discovery.dao.DocumentCache;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.actors.DdsActorController;
import net.es.nsi.pce.discovery.messages.DocumentEvent;
import net.es.nsi.pce.discovery.messages.SubscriptionEvent;
import net.es.nsi.pce.discovery.api.DiscoveryError;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.FilterType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class DdsProvider implements DiscoveryProvider {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Configuration reader.
    private DiscoveryConfiguration configReader;
    
    // In-memory document cache.
    private DocumentCache documentCache;
    
    // The actor system used to send notifications.
    private DdsActorController ddsActorController;
    
    // In-memory subscription cache indexed by subscriptionId.
    private Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
 
    public DdsProvider(DiscoveryConfiguration configuration, DocumentCache documentCache, DdsActorController ddsActorController) {
        this.configReader = configuration;
        this.documentCache = documentCache;
        this.ddsActorController = ddsActorController;
    }
    
    public static DiscoveryProvider getInstance() {
        DdsProvider ddsProvider = SpringApplicationContext.getBean("discoveryProvider", DdsProvider.class);
        return ddsProvider;
    }

    @Override
    public void init() {
        // All initialization is now through bean config.
    }
    
    @Override
    public void start() {
        ddsActorController.start();
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
        Cancellable scheduleOnce = ddsActorController.scheduleNotification(se, 5);
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
        ddsActorController.sendNotification(se);
        
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
    public void processNotification(NotificationType notification) throws IllegalArgumentException, EntityExistsException, NotFoundException {
        log.debug("processNotification: event=" + notification.getEvent() + ", discovered=" + notification.getDiscovered());
        
        // TODO: We discard the event type and discovered time, however, the
        // discovered time could be used for an audit.  Perhaps save it?
        
        // Determine if we have already seen this document event.
        DocumentType document = notification.getDocument();
        if (document == null) {
            log.debug("processNotification: Document null.");
            return;
        }
        
        String documentId = Document.documentId(document);
        
        Document entry = documentCache.get(documentId);
        if (entry == null) {
            // This must be the first time we have seen the document so add it
            // into our cache.
            log.debug("processNotification: new documentId=" + documentId);
            addDocument(document);
        }
        else {
            // We have seen the document before.
            log.debug("processNotification: update documentId=" + documentId);
            try {
                updateDocument(document);
            }
            catch (InvalidVersionException ex) {
                // This is an old document version so discard.
                log.debug("processNotification: old document version documentId=" + documentId);
            }
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
        
        // Validate basic fields.
        if (request.getNsa() == null || request.getNsa().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, document.getId(), "nsa");
            throw new IllegalArgumentException(error); 
        }

        if (request.getType() == null || request.getType().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, document.getId(), "type");
            throw new IllegalArgumentException(error); 
        }

        if (request.getId() == null || request.getId().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, document.getId(), "id");
            throw new IllegalArgumentException(error); 
        }

        if (request.getVersion() == null || !request.getVersion().isValid()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, document.getId(), "version");
            throw new IllegalArgumentException(error); 
        }

        if (request.getExpires() == null || !request.getExpires().isValid()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, document.getId(), "expires");
            throw new IllegalArgumentException(error); 
        }

        // This is a new document so add it into the document space.
        try {
            documentCache.put(document.getId(), document);
        }
        catch (JAXBException | IOException ex) {
            log.error("addDocument: failed to add document to document store", ex);
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_INVALID, "document", document.getId());
            throw new IllegalArgumentException(error);             
        }
        
        // Route a new document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.NEW);
        de.setDocument(document);
        ddsActorController.sendNotification(de);
        return document;
    }
    
    @Override
    public Document updateDocument(String nsa, String type, String id, DocumentType request) throws IllegalArgumentException, NotFoundException, InvalidVersionException {
        // Create a document identifier to look up in our documet table.
        String documentId = Document.documentId(nsa, type, id);
        
        log.debug("updateDocument: documentId=" + documentId);
        
        // See if we have a document under this id.
        Document document = documentCache.get(documentId);
        if (document == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", documentId);
            throw new NotFoundException(error);            
        }
        
        log.debug("updateDocument: found documentId=" + documentId);

        // Validate basic fields.
        if (request.getNsa() == null || request.getNsa().isEmpty() || !request.getNsa().equalsIgnoreCase(document.getDocument().getNsa())) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, documentId, "nsa");
            throw new IllegalArgumentException(error); 
        }

        if (request.getType() == null || request.getType().isEmpty() || !request.getType().equalsIgnoreCase(document.getDocument().getType())) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, documentId, "type");
            throw new IllegalArgumentException(error); 
        }

        if (request.getId() == null || request.getId().isEmpty() || !request.getId().equalsIgnoreCase(document.getDocument().getId())) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, documentId, "id");
            throw new IllegalArgumentException(error); 
        }

        if (request.getVersion() == null || !request.getVersion().isValid()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, documentId, "version");
            throw new IllegalArgumentException(error); 
        }

        if (request.getExpires() == null || !request.getExpires().isValid()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, documentId, "expires");
            throw new IllegalArgumentException(error); 
        }
        
        // Make sure this is a new version of the document.
        if (request.getVersion().compare(document.getDocument().getVersion()) != DatatypeConstants.GREATER) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_VERSION, request.getId(), "request=" + request.getVersion().toString() + ", actual=" + document.getDocument().getVersion().toString());

            throw new InvalidVersionException(error, request.getVersion(), document.getDocument().getVersion());             
        }
        
        log.debug("updateDocument: received update is good=" + documentId);
   
        Document newDoc = new Document(request);
        newDoc.setLastDiscovered(new Date());
        try {
            documentCache.update(documentId, newDoc);
        }
        catch (JAXBException jaxb) {
            log.error("updateDocument: Failed to generate document XML, documentId=" + documentId);
        }
        catch (IOException io) {
            log.error("updateDocument: Failed to write document to cache, documentId=" + documentId);
        }

        // Route a update document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.UPDATED);
        de.setDocument(newDoc);
        ddsActorController.sendNotification(de);
        
        return document;
    }
    
    @Override
    public Document updateDocument(DocumentType request) throws IllegalArgumentException, NotFoundException, InvalidVersionException {
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
    public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException, NotFoundException {
        // Seed the results.
        Collection<Document> results = documentCache.values();

        // This is the primary search value.  Make sure it is present.
        if (nsa != null && !nsa.isEmpty()) {
            results = getDocumentsByNsa(nsa, results);
            if (results == null || results.isEmpty()) {
                String error = DiscoveryError.getErrorString(DiscoveryError.NOT_FOUND, "nsa", nsa);
                throw new NotFoundException(error);   
            }
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
        ddsActorController.shutdown();
    }

    /**
     * @return the configReader
     */
    public DiscoveryConfiguration getConfigReader() {
        return configReader;
    }

    /**
     * @param configReader the configReader to set
     */
    public void setConfigReader(DiscoveryConfiguration configReader) {
        this.configReader = configReader;
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
            catch (JAXBException | IOException ex) {
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
                    catch (IllegalArgumentException | NotFoundException | InvalidVersionException ex) {
                        log.error("loadDocuments: Could not update document " + filename, ex);
                        continue;                                
                    }                                
                }

                log.debug("loadDocuments: updated document " + filename);
            }
        }
    }
}
