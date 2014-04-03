/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.jaxb.PeerURLType;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import net.es.nsi.pce.schema.MediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class RegistrationRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsProvider provider;
    private Router router;
    private Cancellable schedule;

    public RegistrationRouter(DdsProvider provider) {
        this.provider = provider;
    }

    @Override
    public void preStart() {
        log.debug("preStart: entering.");
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < provider.getConfigReader().getActorPool(); i++) {
            ActorRef r = getContext().actorOf(Props.create(RegistrationActor.class, provider));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
        
        RegistrationEvent event = new RegistrationEvent();
        event.setEvent(RegistrationEvent.Event.Register);
        schedule = provider.getActorSystem().scheduler().scheduleOnce(Duration.create(10, TimeUnit.SECONDS), this.getSelf(), event, provider.getActorSystem().dispatcher(), null);
        log.debug("preStart: exiting.");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("onReceive: entering.");
        if (msg instanceof RegistrationEvent) {
            RegistrationEvent re = (RegistrationEvent) msg;
            log.debug("RegistrationRouter: event " + re.getEvent());
            if (re.getEvent() == RegistrationEvent.Event.Register) {
                // This is our first time through after initialization.
                routeRegister();
            }
            if (re.getEvent() == RegistrationEvent.Event.Audit) {
                // A regular audit event.
                routeAudit();
            }
            else if (re.getEvent() == RegistrationEvent.Event.Delete) {
                // We are shutting down so clean up.
                routeShutdown();
            }
        }
        else if (msg instanceof Terminated) {
            log.debug("RegistrationRouter: terminate event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.debug("RegistrationRouter: unhandled event.");
            unhandled(msg);
        }

        RegistrationEvent event = new RegistrationEvent();
        event.setEvent(RegistrationEvent.Event.Audit);
        schedule = provider.getActorSystem().scheduler().scheduleOnce(Duration.create(provider.getConfigReader().getAuditInterval(), TimeUnit.SECONDS), this.getSelf(), event, provider.getActorSystem().dispatcher(), null);
        log.debug("onReceive: exiting.");
    }
    
    private void routeRegister() {
        // Check the list of discovery URL against what we already have.
        Set<PeerURLType> discoveryURL = provider.getConfigReader().getDiscoveryURL();

        for (PeerURLType url : discoveryURL) {
            // We have not seen this before.
            if (url.getType().equalsIgnoreCase(MediaTypes.NSI_DDS_V1_XML)) {
                RemoteSubscription sub = new RemoteSubscription();
                sub.setDdsURL(url.getValue());
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Register);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf());
            }
        }
    }
    
    private void routeAudit() {
        // Check the list of discovery URL against what we already have.
        Set<PeerURLType> discoveryURL = provider.getConfigReader().getDiscoveryURL();
        Set<String> subscriptionURL = provider.remoteSubscriptionKeys();

        for (PeerURLType url : discoveryURL) {
            if (!url.getType().equalsIgnoreCase(MediaTypes.NSI_DDS_V1_XML)) {
                continue;
            }
            
            // See if we already have seen this URL.            
            RemoteSubscription sub = provider.getRemoteSubscription(url.getValue());
            if (sub == null) {
                // We have not seen this before.
                sub = new RemoteSubscription();
                sub.setDdsURL(url.getValue());
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Register);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf());
            }
            else {
                // We have seen this URL before.
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Update);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf()); 

                // Remove from the existing list as processed.
                subscriptionURL.remove(url.getValue());
            }
        }

        // Now we see if there are any URL we missed from the old list and
        // unsubscribe them since we seem to no longer be interested.
        for (String url : subscriptionURL) {
            RemoteSubscription sub = provider.getRemoteSubscription(url);
            if (sub != null) { // Should always be true unless modified while we are processing.
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Delete);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf());
            }
        }
    }
    
    private void routeShutdown() {
        for (String url : provider.remoteSubscriptionKeys()) {
            RemoteSubscription sub = provider.getRemoteSubscription(url);
            if (sub != null) { // Should always be true unless modified while we are processing.
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Delete);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf());
            }
        }
    }
}
