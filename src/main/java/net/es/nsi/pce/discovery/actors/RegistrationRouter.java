package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.dao.RemoteSubscriptionCache;
import net.es.nsi.pce.discovery.messages.RegistrationEvent;
import net.es.nsi.pce.discovery.messages.RemoteSubscription;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.jaxb.PeerURLType;
import net.es.nsi.pce.discovery.messages.StartMsg;
import net.es.nsi.pce.schema.NsiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class RegistrationRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private DiscoveryConfiguration discoveryConfiguration;
    private int poolSize;
    private long interval;
    private Router router;

    private RemoteSubscriptionCache remoteSubscriptionCache;

    public RegistrationRouter(DdsActorSystem ddsActorSystem,
            DiscoveryConfiguration discoveryConfiguration,
            RemoteSubscriptionCache remoteSubscriptionCache) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
        this.remoteSubscriptionCache = remoteSubscriptionCache;
    }

    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < getPoolSize(); i++) {
            ActorRef r = getContext().actorOf(Props.create(RegistrationActor.class, discoveryConfiguration));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void onReceive(Object msg) {
        // Check to see if we got the go ahead to start registering.
        if (msg instanceof StartMsg) {
            // Create a Register event to start us off.
            RegistrationEvent event = new RegistrationEvent();
            event.setEvent(RegistrationEvent.Event.Register);
            msg = event;
        }

        if (msg instanceof RegistrationEvent) {
            RegistrationEvent re = (RegistrationEvent) msg;
            if (re.getEvent() == RegistrationEvent.Event.Register) {
                // This is our first time through after initialization.
                log.debug("RegistrationRouter: routeRegister");
                routeRegister();
            }
            if (re.getEvent() == RegistrationEvent.Event.Audit) {
                // A regular audit event.
                log.debug("RegistrationRouter: routeAudit");
                routeAudit();
            }
            else if (re.getEvent() == RegistrationEvent.Event.Delete) {
                // We are shutting down so clean up.
                log.debug("RegistrationRouter: routeShutdown");
                routeShutdown();
            }
        }
        else if (msg instanceof Terminated) {
            log.error("RegistrationRouter: Terminated event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.error("RegistrationRouter: unhandled event.");
            unhandled(msg);
        }

        RegistrationEvent event = new RegistrationEvent();
        event.setEvent(RegistrationEvent.Event.Audit);
        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), event, ddsActorSystem.getActorSystem().dispatcher(), null);
    }

    private void routeRegister() {
        // We need to register invoke a registration actor for each remote DDS
        // we are peering with.
        Set<PeerURLType> discoveryURL = discoveryConfiguration.getDiscoveryURL();

        for (PeerURLType url : discoveryURL) {
            if (url.getType().equalsIgnoreCase(NsiConstants.NSI_DDS_V1_XML)) {
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
        Set<PeerURLType> discoveryURL = discoveryConfiguration.getDiscoveryURL();
        Set<String> subscriptionURL = Sets.newHashSet(remoteSubscriptionCache.keySet());

        for (PeerURLType url : discoveryURL) {
            if (!url.getType().equalsIgnoreCase(NsiConstants.NSI_DDS_V1_XML)) {
                continue;
            }

            // See if we already have seen this URL.
            RemoteSubscription sub = remoteSubscriptionCache.get(url.getValue());
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
            RemoteSubscription sub = remoteSubscriptionCache.get(url);
            if (sub != null) { // Should always be true unless modified while we are processing.
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Delete);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf());
            }
        }
    }

    private void routeShutdown() {
        for (String url : remoteSubscriptionCache.keySet()) {
            RemoteSubscription sub = remoteSubscriptionCache.get(url);
            if (sub != null) { // Should always be true unless modified while we are processing.
                RegistrationEvent regEvent = new RegistrationEvent();
                regEvent.setEvent(RegistrationEvent.Event.Delete);
                regEvent.setSubscription(sub);
                router.route(regEvent, getSelf());
            }
        }
    }

    /**
     * @return the poolSize
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * @param poolSize the poolSize to set
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }
}
