package net.es.nsi.pce.discovery.provider;

import akka.actor.Cancellable;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.pce.discovery.api.DiscoveryError;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.schema.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class Subscription implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String SUBSCRIPTIONS_URL = "/subscriptions/";
    private ObjectFactory factory;
    private String id;
    private String encoding;
    private SubscriptionType subscription;
    private Date lastModified = new Date();
    private Cancellable action;

    public Subscription(SubscriptionRequestType request, String encoding) throws IllegalArgumentException {
        // Create a Dsicovery JAXB factory.
        factory = new ObjectFactory();
        
        // The unique subcription id is defined on the server side.
        id = UUID.randomUUID().toString();
        
        // This is the encoding in which the client would like notifications sent.
        this.encoding = encoding;
        
        // We take the subscription request parameters passed and complete the
        // subscription resource.
        subscription = factory.createSubscriptionType();
        subscription.setId(id);
        
        // This is the direct URL to the subscription resource.
        subscription.setHref(SUBSCRIPTIONS_URL + id);
        
        // We manage the version of subscription resources.
        try {
            subscription.setVersion(XmlUtilities.xmlGregorianCalendar());
        } catch (DatatypeConfigurationException ex) {
            // Log and eat the error.
            log.error("addSubscription: failed to set version", ex);
        }

        // Validate the callback parameter was provided.
        if (request.getCallback() == null || request.getCallback().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "callback");
            throw new IllegalArgumentException(error);            
        }
        
        // Make sure it can be parsed into a URL.
        try {
            URL url = new URL(request.getCallback());
        }
        catch (MalformedURLException ex) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "callback");
            throw new IllegalArgumentException(error);   
        }

        subscription.setCallback(request.getCallback());
        
        // Will need to revisit what is valid later.
        subscription.setFilter(request.getFilter());
        
        if (request.getRequesterId() == null || request.getRequesterId().isEmpty()) {        
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "callback");
            throw new IllegalArgumentException(error);   
        }
        subscription.setRequesterId(request.getRequesterId());
        
        subscription.getAny().addAll(request.getAny());
        subscription.getOtherAttributes().putAll(request.getOtherAttributes());
    }

    /**
     * @return the subscription
     */
    public SubscriptionType getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(SubscriptionType subscription) {
        this.subscription = subscription;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return the event
     */
    public Cancellable getAction() {
        return action;
    }

    /**
     * @param event the event to set
     */
    public void setAction(Cancellable event) {
        this.action = event;
    }
    
    public void update(SubscriptionRequestType request, String encoding) throws IllegalArgumentException {
        if (request == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "subscription", "subscriptionRequest");
            throw new IllegalArgumentException(error);            
        }
        
        if (encoding == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "PUT", "encoding");
            throw new IllegalArgumentException(error);            
        }

        this.encoding = encoding;
        lastModified.setTime(System.currentTimeMillis());
        subscription.setRequesterId(request.getRequesterId());
        subscription.setFilter(request.getFilter());
        subscription.setCallback(request.getCallback());
        subscription.getAny().addAll(request.getAny());
        subscription.getOtherAttributes().putAll(request.getOtherAttributes());
        try {
            subscription.setVersion(XmlUtilities.xmlGregorianCalendar());
        } catch (DatatypeConfigurationException ex) {
            // Log and eat the error.
            log.error("addSubscription: failed to set version", ex);
        }
    }
}
