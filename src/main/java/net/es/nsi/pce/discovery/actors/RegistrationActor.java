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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.jaxb.ErrorType;
import net.es.nsi.pce.discovery.jaxb.FilterCriteriaType;
import net.es.nsi.pce.discovery.jaxb.FilterType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.jaxb.SubscriptionListType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.discovery.util.UrlHelper;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.schema.NsiConstants;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RegistrationActor extends UntypedActor {
    private static final String NOTIFICATIONS_URL = "notifications";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private DiscoveryConfiguration discoveryConfiguration;
    private RemoteSubscriptionCache remoteSubscriptionCache;
    private RestClient restClient;

    public RegistrationActor(DiscoveryConfiguration discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
        this.remoteSubscriptionCache = RemoteSubscriptionCache.getInstance();
        this.restClient = RestClient.getInstance();
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
        String baseURL = discoveryConfiguration.getBaseURL();
        URL url;
        if (!baseURL.endsWith("/")) {
            baseURL = baseURL + "/";
        }
        url = new URL(baseURL);
        url = new URL(url, NOTIFICATIONS_URL);
        return url.toExternalForm();
    }

    private void register(RegistrationEvent event) {
        String remoteDdsURL = event.getSubscription().getDdsURL();

        // We will register for all events on all documents.
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        SubscriptionRequestType request = factory.createSubscriptionRequestType();
        request.setFilter(filter);
        request.setRequesterId(discoveryConfiguration.getNsaId());

        try {
            request.setCallback(getNotificationURL());
        }
        catch (MalformedURLException mx) {
            log.error("RegistrationActor.register: failed to get my notification callback URL", mx);
            log.error("RegistrationActor.register: failed registration for " + remoteDdsURL);
            return;
        }

        Client client = restClient.get();

        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions");
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);
        Response response;
        try {
            log.debug("RegistrationActor: registering with remote DDS " + remoteDdsURL);
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxb) {}, NsiConstants.NSI_DDS_V1_XML));
        }
        catch (Exception ex) {
            log.error("RegistrationActor.register: endpoint " + remoteDdsURL, ex);
            //client.close();
            return;
        }

        // If this failed then we log the issue and do not save the subscription information.
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            log.error("RegistrationActor.register: failed to create subscription " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.register: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            response.close();
            //client.close();
            return;
        }

        // Looks like we were successful so save the subscription information.
        SubscriptionType subscription = response.readEntity(SubscriptionType.class);
        response.close();
        //client.close();

        log.debug("RegistrationActor.register: created subscription " + subscription.getId() + ", href=" + subscription.getHref());

        event.getSubscription().setSubscription(subscription);
        event.getSubscription().setLastModified(response.getLastModified());
        remoteSubscriptionCache.add(event.getSubscription());

        // Now that we have registered a new subscription make sure we clean up
        // and old ones that may exist on the remote DDS.
        deleteOldSubscriptions(remoteDdsURL, subscription.getId());
    }

    private void deleteOldSubscriptions(String remoteDdsURL, String id) {
        Client client = restClient.get();

        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions").queryParam("requesterId", discoveryConfiguration.getNsaId());
        Response response;
        try {
            log.debug("deleteOldSubscriptions: querying subscriptions on remote DDS " + remoteDdsURL);
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).get();
        }
        catch (Exception ex) {
            log.error("deleteOldSubscriptions: failed for endpoint " + remoteDdsURL, ex);
            //client.close();
            return;
        }

        // If this failed then we log the issue hope for the best.  Duplicate
        // notification are not an issue.
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.error("deleteOldSubscriptions: failed to retrieve list of subscriptions " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("deleteOldSubscriptions: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            response.close();
            //client.close();
            return;
        }

        // Looks like we were successful so save the subscription information.
        SubscriptionListType subscriptions = response.readEntity(SubscriptionListType.class);
        response.close();
        //client.close();

        // For each subscription returned registered to our nsaId we check to
        // see if it is the one we just registered (current subscription).  If
        // it is not we delete the subscription.
        for (SubscriptionType subscription : subscriptions.getSubscription()) {
            if (!id.equalsIgnoreCase(subscription.getId())) {
                // Found one we need to remove.
                log.debug("deleteOldSubscriptions: found stale subscription " + subscription.getHref() + " on DDS " + remoteDdsURL);
                deleteSubscription(remoteDdsURL, subscription.getHref());
            }
        }
    }

    private void update(RegistrationEvent event) {
        Client client = restClient.get();

        // First we retrieve the remote subscription to see if it is still
        // valid.  If it is not then we register again, otherwise we leave it
        // alone for now.
        RemoteSubscription subscription = remoteSubscriptionCache.get(event.getSubscription().getDdsURL());
        String remoteSubscriptionURL = subscription.getSubscription().getHref();

        // Check to see if the remote subscription URL is absolute or relative.
        WebTarget webTarget;
        if (UrlHelper.isAbsolute(remoteSubscriptionURL)) {
            webTarget = client.target(remoteSubscriptionURL);
        }
        else {
            webTarget = client.target(subscription.getDdsURL()).path(remoteSubscriptionURL);
        }

        Response response = null;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).header("If-Modified-Since", DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.update: failed to update subscription " + subscription.getSubscription().getHref(), ex);
            if (response != null) {
                response.close();
            }
            //client.close();
            return;
        }

        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            // The subscription exists and has not been modified.
            log.debug("RegistrationActor.update: subscription " + webTarget.getUri().toASCIIString() + " not modified.");
        }
        else if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            // The subscription exists so we do not need to do anything.
            subscription.setLastModified(response.getLastModified());
            log.debug("RegistrationActor.update: subscription " + webTarget.getUri().toASCIIString() + " modified.");
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            // Looks like our subscription was removed. We need to add it back in.
            log.debug("RegistrationActor.update: subscription " + webTarget.getUri().toASCIIString() + " does not exists and will be recreated.");

            // Remove the stored subscription since a new one will be created.
            remoteSubscriptionCache.remove(subscription.getDdsURL());
            register(event);
        }
        else {
            // Some other error we cannot handle at the moment.
            log.error("RegistrationActor.update: failed to update subscription " + webTarget.getUri().toASCIIString() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.update: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
        }
        //client.close();
        response.close();
    }

    private void delete(RegistrationEvent event) {

        RemoteSubscription subscription = event.getSubscription();
        if (deleteSubscription(subscription.getDdsURL(), subscription.getSubscription().getHref())) {
            remoteSubscriptionCache.remove(subscription.getDdsURL());
        }
    }

    private boolean deleteSubscription(String remoteDdsURL, String remoteSubscriptionURL) {
        Client client = restClient.get();

        // Check to see if the remote subscription URL is absolute or relative.
        WebTarget webTarget;
        if (UrlHelper.isAbsolute(remoteSubscriptionURL)) {
            webTarget = client.target(remoteSubscriptionURL);
        }
        else {
            webTarget = client.target(remoteDdsURL).path(remoteSubscriptionURL);
        }

        Response response = null;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).delete();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.delete: failed to delete subscription " + remoteDdsURL, ex);
            //client.close();
            if (response != null) {
                response.close();
            }
            return false;
        }

        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            // Successfully deleted the subscription.
            log.debug("RegistrationActor.delete: deleted " + remoteSubscriptionURL);
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            log.error("RegistrationActor.delete: subscription did not exist " + remoteSubscriptionURL);
        }
        else {
            log.error("RegistrationActor.delete: failed to delete subscription " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.delete: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }

            response.close();
            //client.close();
            return false;
        }

        response.close();
        //client.close();

        return true;
    }
}