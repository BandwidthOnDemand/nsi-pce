/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.UntypedActor;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class ConfigurationActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsProvider provider;

    public ConfigurationActor(DdsProvider provider) {
        this.provider = provider;
    }
    @Override
    public void preStart() {
        log.debug("preStart: entering.");  
        TimerMsg message = new TimerMsg();
        provider.getActorSystem().scheduler().scheduleOnce(Duration.create(60, TimeUnit.SECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);
        log.debug("preStart: exiting.");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("onReceive: entering.");
        if (msg instanceof TimerMsg) {
            TimerMsg event = (TimerMsg) msg;
            log.debug("onReceive: processing.");

            try {
                provider.getConfigReader().load();
            }
            catch (IllegalArgumentException | JAXBException | FileNotFoundException | NullPointerException ex) {
                log.error("onReceive: Configuration load failed.", ex);
            }

            provider.getActorSystem().scheduler().scheduleOnce(Duration.create(provider.getConfigReader().getAuditInterval(), TimeUnit.SECONDS), this.getSelf(), event, provider.getActorSystem().dispatcher(), null);        

        } else {
            unhandled(msg);
        }
        log.debug("onReceive: exiting.");
    }

}
