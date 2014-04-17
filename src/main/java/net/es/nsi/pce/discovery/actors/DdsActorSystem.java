/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.agole.AgoleDiscoveryRouter;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.dao.DocumentCache;
import net.es.nsi.pce.discovery.gangofthree.Gof3DiscoveryRouter;
import net.es.nsi.pce.discovery.messages.StartMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class DdsActorSystem {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Configuration reader.
    private DiscoveryConfiguration configReader;
    private DocumentCache documentCache;
    private ActorSystem actorSystem;
    private ActorRef notificationRouter;
    private ActorRef registrationRouter;
    private ActorRef localDocumentActor;
    private ActorRef configurationActor;
    private ActorRef cacheActor;
    private ActorRef gangOfThreeRouter;
    private ActorRef agoleRouter;
    
    private List<ActorRef> startList = new ArrayList<>();
    
    public DdsActorSystem(DiscoveryConfiguration configReader, DocumentCache documentCache) {
        this.configReader = configReader;
        this.documentCache = documentCache;
    }
    
    public void init() throws IllegalArgumentException, JAXBException, FileNotFoundException {
        /*ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> myClass = myClassLoader.loadClass("net.es.nsi.pce.discovery.actors.ConfigurationActor");
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DdsActorSystem.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        // Initialize the AKKA actor system for the PCE and subsystems.
        log.info("DdsActorSystem: Initializing actor framework...");
        actorSystem = ActorSystem.create("NSI-DISCOVERY");
        log.info("DdsActorSystem: ... Actor framework initialized.");
        
        // Each actor must take the parameters (DdsActorSystem ddsActorSystem,
        // int poolSize, long interval) which are configured via Spring.
        int poolSize = configReader.getActorPool();
        long interval = configReader.getAuditInterval();
        
        // Initialize the configuration refresh actor.
        log.info("DdsActorSystem: Initializing configuration actor.");
        configurationActor = actorSystem.actorOf(Props.create(ConfigurationActor.class, this, poolSize, interval), "discovery-configuration-actor");
        log.info("DdsActorSystem:... Configuration actor initialized.");
        
        // Load our local documents only if a local directory is configured.
        if (documentCache.isUseDocuments()) {
            log.info("DdsActorSystem: Initializing local document repository.");
            localDocumentActor = actorSystem.actorOf(Props.create(LocalDocumentActor.class, this, poolSize, interval), "discovery-document-watcher");
            log.info("DdsActorSystem:... Local document repository initialized.");
        }

        // Initialize document expiry actor.
        log.info("DdsActorSystem: Initializing document expiry actor.");
        localDocumentActor = actorSystem.actorOf(Props.create(DocumentExpiryActor.class, this, poolSize, interval), "discovery-expiry-watcher");
        log.info("DdsActorSystem:... Document expiry actor initialized.");

        // Initialize the subscription notification actors.
        log.info("DdsActorSystem: Initializing notification router.");
        notificationRouter = actorSystem.actorOf(Props.create(NotificationRouter.class, this, poolSize, interval), "discovery-notification-router");
        log.info("DdsActorSystem:... Notification router initialized.");

        // Initialize the remote registration actors.  Will need a start message.
        log.info("DdsActorSystem: Initializing peer registration actor...");
        registrationRouter = actorSystem.actorOf(Props.create(RegistrationRouter.class, this, poolSize, interval), "discovery-peer-registration");
        startList.add(registrationRouter);
        log.info("DdsActorSystem:... Peer registration actor initialized.");
        
        // Initialize the Gang of Three actors.  Will need a start message.
        log.info("DdsActorSystem: Initializing Gang of Three actors...");
        gangOfThreeRouter = actorSystem.actorOf(Props.create(Gof3DiscoveryRouter.class, this, poolSize, interval), "discovery-Gof3-registration");
        startList.add(gangOfThreeRouter);
        log.info("DdsActorSystem:... Gang of Three actors initialized.");
        
        // Initialize the A-GOLE actors.  Will need a start message.
        log.info("DdsActorSystem: Initializing A-GOLE actors...");
        agoleRouter = actorSystem.actorOf(Props.create(AgoleDiscoveryRouter.class, this, poolSize, interval), "discovery-AGOLE-registration");
        startList.add(agoleRouter);
        log.info("DdsActorSystem:... A-GOLE actors initialized.");        
    }
    
    public ActorSystem getActorSystem() {
        return actorSystem;
    }
    
    public DiscoveryConfiguration getConfigReader() {
        return configReader;
    }
    
    public Cancellable scheduleNotification(Object message, long delay) {
        Cancellable scheduleOnce = actorSystem.scheduler().scheduleOnce(Duration.create(delay, TimeUnit.SECONDS), notificationRouter, message, actorSystem.dispatcher(), null);
        return scheduleOnce;
    }
    
    public void sendNotification(Object message) {
        notificationRouter.tell(message, null);
    }
    
    public void start() {
        log.info("DdsActorSystem: Starting discovery process...");
        StartMsg msg = new StartMsg();
        for (ActorRef ref : startList) {
            ref.tell(msg, null);
        }
    }
    public void shutdown() {
        log.info("DdsActorSystem: Shutting down actor system...");
        actorSystem.shutdown();
    }
}
