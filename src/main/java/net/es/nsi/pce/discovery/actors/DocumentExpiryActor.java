/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.TimerMsg;
import akka.actor.UntypedActor;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.dao.DocumentCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class DocumentExpiryActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private long interval;

    public DocumentExpiryActor(DdsActorSystem ddsActorSystem, int poolSize, long interval) {
        this.ddsActorSystem = ddsActorSystem;
        this.interval = interval;
    }

    @Override
    public void preStart() {
        log.debug("DocumentExpiryActor: preStart");  
        TimerMsg message = new TimerMsg();
        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(interval, TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        log.debug("DocumentExpiryActor: preStart done");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("DocumentExpiryActor: onReceive");
        if (msg instanceof TimerMsg) {
            TimerMsg event = (TimerMsg) msg;
            log.debug("DocumentExpiryActor: processing.");

            DocumentCache.getInstance().expire();
            
            TimerMsg message = new TimerMsg();
            ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(interval, TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);        

        } else {
            unhandled(msg);
        }
        log.debug("DocumentExpiryActor: onReceive done");
    }

}