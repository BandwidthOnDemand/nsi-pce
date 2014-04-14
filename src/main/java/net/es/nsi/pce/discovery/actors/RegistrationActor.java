/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.dao.RemoteSubscriptionCache;
import net.es.nsi.pce.discovery.messages.RegistrationEvent;
import net.es.nsi.pce.discovery.messages.RemoteSubscription;
import akka.actor.UntypedActor;
import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.jaxb.ErrorType;
import net.es.nsi.pce.discovery.jaxb.FilterCriteriaType;
import net.es.nsi.pce.discovery.jaxb.FilterType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.schema.NsiConstants;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RegistrationActor extends UntypedActor {
    private static final String NOTIFICATIONS_URL = "/discovery/notifications";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private DdsActorSystem ddsActorSystem;
    private RemoteSubscriptionCache remoteSubscriptionCache;
    
    public RegistrationActor(DdsActorSystem ddsActorSystem) {
        this.ddsActorSystem = ddsActorSystem;
        this.remoteSubscriptionCache = RemoteSubscriptionCache.getInstance();
    }

    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof RegistrationEvent) {
            RegistrationEvent event = (RegistrationEvent) msg;
            log.debug("RegistrationActor: event=" + event.getEvent().name());
            
            if (event.getEvent() == RegistrationEvent.Event.Register) {
                register(event);
            }
            else if (event.getEvent() == RegistrationEvent.Event.Update) {
                update(event);
            }
            else if (event.getEvent() == RegistrationEvent.Event.Delete) {
                delete(event);
            }
        } else {
            unhandled(msg);
        }
    }
    
    private String getNotificationURL() throws MalformedURLException {
        URL url = new URL(ddsActorSystem.getConfigReader().getBaseURL());
        url = new URL(url, NOTIFICATIONS_URL);
        return url.toString();
    }
    
    private void register(RegistrationEvent event) {
        // We will register for all events on all documents.
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        SubscriptionRequestType request = factory.createSubscriptionRequestType();
        request.setFilter(filter);
        request.setRequesterId(ddsActorSystem.getConfigReader().getNsaId());
        
        try {
            request.setCallback(getNotificationURL());
        }
        catch (MalformedURLException mx) {
            log.error("RegistrationActor.register: failed to get my notification callback URL", mx);
            log.error("RegistrationActor.register: failed registration for " + event.getSubscription().getDdsURL());
            return;
        }
        
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        
        WebTarget webTarget = client.target(event.getSubscription().getDdsURL()).path("subscriptions");
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);
        Response response;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxb) {}, NsiConstants.NSI_DDS_V1_XML));
        }
        catch (Exception ex) {
            log.error("RegistrationActor.register: endpoint " + event.getSubscription().getDdsURL(), ex);
            client.close();
            return;
        }

        // If this failed then we log the issue and not save the subscription information.
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            log.error("RegistrationActor.register: failed to create subscription " + event.getSubscription().getDdsURL() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.register: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            client.close();
            return;
        }
        
        // Looks like we were successful so save the subscription information.
        SubscriptionType subscription = response.readEntity(SubscriptionType.class);
        client.close();
        log.debug("RegistrationActor.register: created subscription " + subscription.getId() + ", href=" + subscription.getHref());
        
        event.getSubscription().setSubscription(subscription);
        event.getSubscription().setLastModified(response.getLastModified());
        remoteSubscriptionCache.add(event.getSubscription());
    }
    
    private void update(RegistrationEvent event) {        
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        
        // First we retrieve the remote subscription to see if it is still
        // valid.  If it is not then we register again, otherwise we leave it
        // alone for now.
        RemoteSubscription subscription = event.getSubscription();
        WebTarget webTarget = client.target(subscription.getSubscription().getHref());
        Response response;
        
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).header("If-Modified-Since", DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.update: failed to update subscription " + subscription.getSubscription().getHref(), ex);
            client.close();
            return;
        }
        
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            // The subscription exists and has not been modified.
            log.debug("RegistrationActor.update: subscription " + subscription.getSubscription().getHref() + " not modified.");
        }
        else if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            // The subscription exists so we do not need to do anything.
            subscription.setLastModified(response.getLastModified());
            log.debug("RegistrationActor.update: subscription " + subscription.getSubscription().getHref() + " modified.");
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            // Looks like our subscription was removed. We need to add it back in.
            log.debug("RegistrationActor.update: subscription " + subscription.getSubscription().getHref() + " does not exists and will be recreated.");
            
            // Remove the stored subscription since a new one will be created.
            remoteSubscriptionCache.remove(event.getSubscription().getDdsURL());
            register(event);
        }
        else {
            // Some other error we cannot handle at the moment.
            log.error("RegistrationActor.update: failed to update subscription " + subscription.getSubscription().getHref() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.update: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
        }
        client.close();
    }
    
    private void delete(RegistrationEvent event) {
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(event.getSubscription().getSubscription().getHref());
        
        Response response;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).delete();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.delete: failed to delete subscription " + event.getSubscription().getDdsURL(), ex);
            client.close();
            return;
        }

        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            // Successfully deleted the subscription.
            log.debug("RegistrationActor.delete: deleted " + event.getSubscription().getSubscription().getHref());
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            log.error("RegistrationActor.delete: subscription did not exist " + event.getSubscription().getSubscription().getHref());
        }
        else {
            log.error("RegistrationActor.delete: failed to delete subscription " + event.getSubscription().getDdsURL() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.delete: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            client.close();
            return;
        }
        
        client.close();
        remoteSubscriptionCache.remove(event.getSubscription().getDdsURL());        
    }
}