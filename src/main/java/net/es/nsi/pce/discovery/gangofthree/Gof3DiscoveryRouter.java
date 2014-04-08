/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.gangofthree;

import net.es.nsi.pce.discovery.messages.TimerMsg;
import net.es.nsi.pce.discovery.actors.*;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
public class Gof3DiscoveryRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private int poolSize;
    private long interval;
    private Router router;
    private Map<String, Gof3DiscoveryMsg> discovery = new ConcurrentHashMap<>();

    public Gof3DiscoveryRouter(DdsActorSystem ddsActorSystem, int poolSize, long interval) {
        this.ddsActorSystem = ddsActorSystem;
        this.poolSize = poolSize;
        this.interval = interval;
    }

    @Override
    public void preStart() {
        log.debug("preStart: entering.");
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            ActorRef r = getContext().actorOf(Props.create(Gof3DiscoveryActor.class));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
        log.debug("preStart: exiting.");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("onReceive: entering.");
        TimerMsg message = new TimerMsg();

        // Check to see if we got the go ahead to start registering.
        if (msg instanceof StartMsg) {
            // Create a Register event to start us off.
            msg = message;
        }

        if (msg instanceof TimerMsg) {
            log.debug("onReceive: timer event.");
            routeTimerEvent();
        }
        else if (msg instanceof Gof3DiscoveryMsg) {
            Gof3DiscoveryMsg incoming = (Gof3DiscoveryMsg) msg;
            
            log.debug("onReceive: discovery update for nsaId=" + incoming.getNsaId());

            discovery.put(incoming.getNsaURL(), incoming);
        }
        else if (msg instanceof Terminated) {
            log.debug("onReceive: terminate event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(Gof3DiscoveryActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.debug("onReceive: unhandled event.");
            unhandled(msg);
        }

        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(interval, TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        log.debug("onReceive: exiting.");
    }
    
    private void routeTimerEvent() {
        log.debug("routeTimerEvent: entering.");
        Set<PeerURLType> discoveryURL = ddsActorSystem.getConfigReader().getDiscoveryURL();
        Set<String> notSent = discovery.keySet();

        for (PeerURLType url : discoveryURL) {
            if (!url.getType().equalsIgnoreCase(NsiConstants.NSI_NSA_V1)) {
                continue;
            }
            
            log.debug("routeTimerEvent: url=" + url.getValue());
            
            Gof3DiscoveryMsg msg = discovery.get(url.getValue());
            if (msg == null) {
                msg = new Gof3DiscoveryMsg();
                msg.setNsaURL(url.getValue());
            }

            router.route(msg, getSelf());
            notSent.remove(msg.getNsaURL());
        }
        
        // Clean up the entries no longer in the configuration.
        for (String url : notSent) {
            log.debug("routeTimerEvent: entry no longer needed, url=" + url);
            discovery.remove(url);
        }

        log.debug("routeTimerEvent: exiting.");
    }
}