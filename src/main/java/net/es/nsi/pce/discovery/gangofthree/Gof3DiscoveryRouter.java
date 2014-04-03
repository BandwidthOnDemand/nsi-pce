/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.gangofthree;

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
import net.es.nsi.pce.discovery.provider.DdsProvider;
import net.es.nsi.pce.schema.MediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class Gof3DiscoveryRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsProvider provider;
    private Router router;
    private Map<String, Gof3DiscoveryMsg> discovery = new ConcurrentHashMap<>();

    public Gof3DiscoveryRouter(DdsProvider provider) {
        this.provider = provider;
    }

    @Override
    public void preStart() {
        log.debug("preStart: entering.");
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < provider.getConfigReader().getActorPool(); i++) {
            ActorRef r = getContext().actorOf(Props.create(Gof3DiscoveryActor.class, provider));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
        
        TimerMsg message = new TimerMsg();
        provider.getActorSystem().scheduler().scheduleOnce(Duration.create(100, TimeUnit.MILLISECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);
        log.debug("preStart: exiting.");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("onReceive: entering.");
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

        TimerMsg message = new TimerMsg();
        provider.getActorSystem().scheduler().scheduleOnce(Duration.create(provider.getConfigReader().getAuditInterval(), TimeUnit.SECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);
        log.debug("onReceive: exiting.");
    }
    
    private void routeTimerEvent() {
        log.debug("routeTimerEvent: entering.");
        Set<PeerURLType> discoveryURL = provider.getConfigReader().getDiscoveryURL();
        Set<String> notSent = discovery.keySet();

        for (PeerURLType url : discoveryURL) {
            if (!url.getType().equalsIgnoreCase(MediaTypes.NSI_NSA_V1)) {
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