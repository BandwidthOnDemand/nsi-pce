/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.TimerMsg;
import akka.actor.UntypedActor;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class LocalDocumentActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private long interval;

    public LocalDocumentActor(DdsActorSystem ddsActorSystem, int poolSize, long interval) {
        this.ddsActorSystem = ddsActorSystem;
        this.interval = interval;
    }

    @Override
    public void preStart() {
        log.debug("preStart: entering.");  
        TimerMsg message = new TimerMsg();
        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(interval, TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        log.debug("entering: exiting.");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("onReceive: entering.");
        if (msg instanceof TimerMsg) {
            TimerMsg event = (TimerMsg) msg;
            log.debug("onReceive: processing.");
            String directory = ddsActorSystem.getConfigReader().getDocuments();
            if (directory != null && !directory.isEmpty()) {
                DdsProvider.getInstance().loadDocuments(ddsActorSystem.getConfigReader().getDocuments());
            }
            TimerMsg message = new TimerMsg();
            ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(interval, TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);        

        } else {
            unhandled(msg);
        }
        log.debug("onReceive: exiting.");
    }
}