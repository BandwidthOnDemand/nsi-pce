/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.Cancellable;
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
    private DdsProvider provider;
    private Cancellable schedule;

    public LocalDocumentActor(DdsProvider provider) {
        this.provider = provider;
    }
    @Override
    public void preStart() {
        log.debug("LocalDocumentActor: preStart");  
        LocalDocumentMsg message = new LocalDocumentMsg();
        schedule = provider.getActorSystem().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);
        log.debug("LocalDocumentActor: preStart done");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("LocalDocumentActor: onReceive");
        if (msg instanceof LocalDocumentMsg) {
            LocalDocumentMsg event = (LocalDocumentMsg) msg;
            log.debug("LocalDocumentActor: processing.");
            provider.loadDocuments(provider.getConfigReader().getDocuments());
            LocalDocumentMsg message = new LocalDocumentMsg();
            schedule = provider.getActorSystem().scheduler().scheduleOnce(Duration.create(provider.getConfigReader().getAuditInterval(), TimeUnit.SECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);        

        } else {
            unhandled(msg);
        }
        log.debug("LocalDocumentActor: onReceive done");
    }

}