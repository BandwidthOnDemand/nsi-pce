package net.es.nsi.pce.discovery.provider;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import javax.persistence.EntityExistsException;
import javax.ws.rs.NotFoundException;
import net.es.nsi.pce.discovery.actors.DocumentEvent;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.FilterType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;

/**
 *
 * @author hacksaw
 */
public interface DiscoveryProvider {

    public void initialize() throws Exception;
    
    public void shutdown();
    
    public Subscription addSubscription(SubscriptionRequestType request, String encoding);
    public Subscription deleteSubscription(String id) throws IllegalArgumentException, NotFoundException;
    public Subscription editSubscription(String id, SubscriptionRequestType request, String encoding) throws IllegalArgumentException, NotFoundException;
    public Subscription getSubscription(String id, Date lastModified) throws IllegalArgumentException, NotFoundException;
    public Collection<Subscription> getSubscriptions(String requesterId, Date lastModified) throws IllegalArgumentException;
    
    public Document addDocument(DocumentType document) throws IllegalArgumentException, EntityExistsException;
    public Document updateDocument(String nsa, String type, String id, DocumentType document) throws IllegalArgumentException, NotFoundException, InvalidVersionException;
    public Document updateDocument(DocumentType request) throws IllegalArgumentException, NotFoundException, InvalidVersionException;
    public Collection<Document> getDocuments(String nsa, String type, String id, Date lastDiscovered);
    public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException;
    public Collection<Document> getDocumentsByNsaAndType(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException;
    public Document getDocument(String nsa, String type, String id, Date lastDiscovered) throws IllegalArgumentException, NotFoundException;
    public Document getDocument(DocumentType document) throws IllegalArgumentException, NotFoundException;
    public Collection<Document> getLocalDocuments(String type, String id, Date lastDiscovered) throws IllegalArgumentException;
    public Collection<Document> getLocalDocumentsByType(String type, String id, Date lastDiscovered) throws IllegalArgumentException;
    public Document getLocalDocument(String type, String id, Date lastDiscovered) throws IllegalArgumentException, NotFoundException;
    
    public void processNotification(NotificationType notification);
    
    public String getNsaId();
    public String getNotificationURL() throws MalformedURLException;
    public String getMediaType();
    
    public Collection<Document> getDocuments(FilterType filter);
    public Collection<Subscription> getSubscriptions(DocumentEvent event);
    
    public void loadDocuments(String path);
    public void expireDocuments();
}
