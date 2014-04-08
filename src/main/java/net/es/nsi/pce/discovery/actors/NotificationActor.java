/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.Notification;
import akka.actor.UntypedActor;
import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.discovery.jaxb.NotificationListType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.provider.DiscoveryProvider;
import net.es.nsi.pce.discovery.provider.Document;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.schema.XmlUtilities;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NotificationActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private DdsActorSystem ddsActorSystem;
    private Client client;
    
    public NotificationActor(DdsActorSystem ddsActorSystem) {
        this.ddsActorSystem = ddsActorSystem;
    }

    @Override
    public void preStart() {
        log.debug("NotificationActor: preStart");
        
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        client = ClientBuilder.newClient(clientConfig);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Notification) {
            Notification notification = (Notification) msg;
            log.debug("NotificationActor: notificationId=" + notification.getSubscription().getId());
            
            NotificationListType list = factory.createNotificationListType();

            Date lastDiscovered = new Date(0);
            for (Document document : notification.getDocuments()) {
                log.debug("NotificationActor: documentId=" + document.getDocument().getId());
                NotificationType notify = factory.createNotificationType();
                notify.setEvent(notification.getEvent());
                notify.setDocument(document.getDocument());
                try {
                    XMLGregorianCalendar discovered = XmlUtilities.longToXMLGregorianCalendar(document.getLastDiscovered().getTime());
                    notify.setDiscovered(discovered);

                    if (lastDiscovered.before(document.getLastDiscovered())) {
                        lastDiscovered = document.getLastDiscovered();
                    }
                }
                catch (Exception ex) {
                    log.error("NotificationActor: discovered date conversion failed", ex);
                }
                list.getNotification().add(notify);
            }
            
            list.setId(notification.getSubscription().getId());
            list.setHref(notification.getSubscription().getSubscription().getHref());
            list.setProviderId(ddsActorSystem.getConfigReader().getNsaId());
            try {
                XMLGregorianCalendar discovered = XmlUtilities.longToXMLGregorianCalendar(lastDiscovered.getTime());
                list.setDiscovered(discovered);
            }
            catch (Exception ex) {
                log.error("NotificationActor: discovered date conversion failed", ex);
            }
            
            final WebTarget webTarget = client.target(notification.getSubscription().getSubscription().getCallback());
            
            JAXBElement<NotificationListType> jaxb = factory.createNotifications(list);
            String mediaType = notification.getSubscription().getEncoding();
            Response response = webTarget.request(mediaType)
                    .post(Entity.entity(new GenericEntity<JAXBElement<NotificationListType>>(jaxb) {}, mediaType));
            
            if (response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
                log.error("NotificationActor: failed notification " + list.getId() + " to client " + notification.getSubscription().getSubscription().getCallback() + ", result = " + response.getStatusInfo().getReasonPhrase());
                // TODO: Tell discovery provider...
                DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
                discoveryProvider.deleteSubscription(notification.getSubscription().getId());
            }
            else if (log.isDebugEnabled()) {
                log.debug("NotificationActor: sent notitifcation " + list.getId() + " to client " + notification.getSubscription().getSubscription().getCallback() + ", result = " + response.getStatusInfo().getReasonPhrase());
            }
        } else {
            unhandled(msg);
        }
    }
}