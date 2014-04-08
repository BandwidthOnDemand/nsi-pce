/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.Notification;
import net.es.nsi.pce.discovery.messages.DocumentEvent;
import net.es.nsi.pce.discovery.messages.SubscriptionEvent;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import net.es.nsi.pce.discovery.provider.DiscoveryProvider;
import net.es.nsi.pce.discovery.provider.Document;
import net.es.nsi.pce.discovery.dao.DocumentCache;
import net.es.nsi.pce.discovery.provider.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NotificationRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private int poolSize;
    private Router router;

    public NotificationRouter(DdsActorSystem ddsActorSystem, int poolSize, long interval) {
        this.ddsActorSystem = ddsActorSystem;
        this.poolSize = poolSize;
    }

    @Override
    public void preStart() {
        log.debug("NotificationRouter: preStart.");
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class, ddsActorSystem));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof DocumentEvent) {
            // We have a document event.
            DocumentEvent de = (DocumentEvent) msg;
            log.debug("NotificationRouter: document event " + de.getEvent() + ", id="  + de.getDocument().getId());
            routeDocumentEvent(de);
        }
        else if (msg instanceof SubscriptionEvent) {
            // We have a subscription event.
            SubscriptionEvent se = (SubscriptionEvent) msg;
            log.debug("NotificationRouter: subscription event id=" + se.getSubscription().getId());           
            routeSubscriptionEvent(se);
        }
        else if (msg instanceof Terminated) {
            log.debug("NotificationRouter: terminate event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.debug("NotificationRouter: unhandled event.");
            unhandled(msg);
        }
    }
    
    private void routeDocumentEvent(DocumentEvent de) {
        DiscoveryProvider discoveryProvider = DdsProvider.getInstance();
        Collection<Subscription> subscriptions = discoveryProvider.getSubscriptions(de);
        
        log.debug("routeDocumentEvent: event=" + de.getEvent() + ", documentId=" + de.getDocument().getId());
        
        // We need to sent the list of matching documents to the callback
        // related to this subscription.  Only send if there is no pending
        // subscription event.
        for (Subscription subscription : subscriptions) {
            log.debug("routeDocumentEvent: subscription=" + subscription.getId() + ", endpoint=" + subscription.getSubscription().getHref());
            if (subscription.getAction() == null) {
                Notification notification = new Notification();
                notification.setEvent(de.getEvent());
                notification.setSubscription(subscription);
                Collection<Document> documents = new ArrayList<>();
                documents.add(de.getDocument());
                notification.setDocuments(documents);
                router.route(notification, getSender());
            }
        }
    }
    
    private void routeSubscriptionEvent(SubscriptionEvent se) {
        
        // TODO: Apply subscription filter to these documents.
        Collection<Document> documents = DocumentCache.getInstance().values();
        
        // Clean up our trigger event.
        log.debug("routeSubscriptionEvent: event=" + se.getEvent() + ", action=" + se.getSubscription().getAction().isCancelled());
        se.getSubscription().setAction(null);
        
        // We need to sent the list of matching documents to the callback
        // related to this subscription.
        Notification notification = new Notification();
        notification.setEvent(DocumentEventType.ALL);
        notification.setSubscription(se.getSubscription());
        notification.setDocuments(documents);
        router.route(notification, getSender());
    }
}
