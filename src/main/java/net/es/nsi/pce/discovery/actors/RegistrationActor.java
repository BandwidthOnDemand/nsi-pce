/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.UntypedActor;
import java.net.MalformedURLException;
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
import net.es.nsi.pce.discovery.provider.NsiDiscoveryServiceProvider;
import net.es.nsi.pce.jersey.RestClient;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RegistrationActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private NsiDiscoveryServiceProvider provider;
    private Client client;
    
    public RegistrationActor(NsiDiscoveryServiceProvider provider) {
        this.provider = provider;
    }

    @Override
    public void preStart() {
        log.debug("RegistrationActor: preStart");
        
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        client = ClientBuilder.newClient(clientConfig);
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
    
    private void register(RegistrationEvent event) {
        // We will register for all events on all documents.
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        SubscriptionRequestType request = factory.createSubscriptionRequestType();
        request.setFilter(filter);
        request.setRequesterId(provider.getNsaId());
        
        try {
            request.setCallback(provider.getNotificationURL());
        }
        catch (MalformedURLException mx) {
            log.error("RegistrationActor.register: failed to get my notification callback URL", mx);
            log.error("RegistrationActor.register: failed registration for " + event.getSubscription().getDdsURL());
            return;
        }

        WebTarget webTarget = client.target(event.getSubscription().getDdsURL()).path("/subscriptions");
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);
        Response response;
        try {
            response = webTarget.request(provider.getMediaType()).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxb) {}, provider.getMediaType()));
        }
        catch (Exception ex) {
            log.error("RegistrationActor.register: endpoint " + event.getSubscription().getDdsURL(), ex);
            return;
        }

        // If this failed then we log the issue and not save the subscription information.
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            log.error("RegistrationActor.register: failed to create subscription " + event.getSubscription().getDdsURL() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.register: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            return;
        }
        
        // Looks like we were successful so save the subscription information.
        SubscriptionType subscription = response.readEntity(SubscriptionType.class);
        log.debug("RegistrationActor.register: created subscription " + subscription.getId() + ", href=" + subscription.getHref());
        
        event.getSubscription().setSubscription(subscription);
        event.getSubscription().setLastModified(response.getLastModified());
        provider.addRemoteSubscription(event.getSubscription());
    }
    
    private void update(RegistrationEvent event) {
        // First we retrieve the remote subscription to see if it is still
        // valid.  If it is not then we register again, otherwise we leave it
        // alone for now.
        RemoteSubscription subscription = event.getSubscription();
        WebTarget webTarget = client.target(subscription.getSubscription().getHref());
        Response response;
        
        try {
            response = webTarget.request(provider.getMediaType()).header("If-Modified-Since", DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.update: failed to update subscription " + subscription.getSubscription().getHref(), ex);
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
            provider.removeRemoteSubscription(event.getSubscription().getDdsURL());
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
    }
    
    private void delete(RegistrationEvent event) {
        WebTarget webTarget = client.target(event.getSubscription().getSubscription().getHref());
        
        Response response;
        try {
            response = webTarget.request(provider.getMediaType()).delete();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.delete: failed to delete subscription " + event.getSubscription().getDdsURL(), ex);
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
            return;
        }
        
        provider.removeRemoteSubscription(event.getSubscription().getDdsURL());        
    }
}