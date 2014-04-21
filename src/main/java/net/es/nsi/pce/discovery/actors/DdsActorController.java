/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.dao.DocumentCache;
import net.es.nsi.pce.discovery.messages.StartMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import scala.concurrent.duration.Duration;
import static net.es.nsi.pce.spring.SpringExtension.SpringExtProvider;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author hacksaw
 */
public class DdsActorController implements ApplicationContextAware {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Configuration reader.
    private DdsActorSystem ddsActorSystem;
    private DiscoveryConfiguration configReader;
    private DocumentCache documentCache;

    private ActorRef notificationRouter;
    private ActorRef registrationRouter;
    private ActorRef localDocumentActor;
    private ActorRef expireDocumentActor;
    private ActorRef configurationActor;
    private ActorRef cacheActor;
    private ActorRef gangOfThreeRouter;
    private ActorRef agoleRouter;
    private ApplicationContext applicationContext;
    
    private List<ActorRef> startList = new ArrayList<>();
    
    public DdsActorController(DdsActorSystem ddsActorSystem, DiscoveryConfiguration configReader, DocumentCache documentCache) {
        this.ddsActorSystem = ddsActorSystem;
        this.configReader = configReader;
        this.documentCache = documentCache;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void init() throws IllegalArgumentException, JAXBException, FileNotFoundException {
        /*ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> myClass = myClassLoader.loadClass("net.es.nsi.pce.discovery.actors.ConfigurationActor");
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DdsActorController.class.getName()).log(Level.SEVERE, null, ex);
        }*/

        // Each actor must take the parameters (DdsActorController ddsActorSystem,
        // int poolSize, long interval) which are configured via Spring.
        int poolSize = configReader.getActorPool();
        long interval = configReader.getAuditInterval();
        
        ActorSystem actorSystem = ddsActorSystem.getActorSystem();
        SpringExtProvider.get(actorSystem).initialize(applicationContext);
        
        // Initialize the configuration refresh actor.
        try {
            log.info("DdsActorController: Initializing configuration actor.");
            configurationActor = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("configurationActor"), "discovery-configuration-actor");
            log.info("DdsActorController:... Configuration actor initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize ConfigurationActor.", ex);
        }

        // Load our local documents only if a local directory is configured.
        try {
            log.info("DdsActorController: Initializing local document repository.");
            localDocumentActor = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("localDocumentActor"), "discovery-document-watcher");
            log.info("DdsActorController:... Local document repository initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize localDocumentActor.", ex);
        }
        
        // Initialize document expiry actor.
        try {
            log.info("DdsActorController: Initializing document expiry actor.");
            expireDocumentActor = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("documentExpiryActor"), "discovery-expiry-watcher");
            log.info("DdsActorController:... Document expiry actor initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize DocumentExpiryActor.", ex);
        }

        // Initialize the subscription notification actors.
        try {
            log.info("DdsActorController: Initializing notification router.");
            notificationRouter = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("notificationRouter"), "discovery-notification-router");
            startList.add(notificationRouter);
            log.info("DdsActorController:... Notification router initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize Notification router.", ex);
        }

        // Initialize the remote registration actors.  Will need a start message.
        try {
            log.info("DdsActorController: Initializing peer registration actor...");
            registrationRouter = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("registrationRouter"), "discovery-peer-registration");
            startList.add(registrationRouter);
            log.info("DdsActorController:... Peer registration actor initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize Peer registration actor.", ex);
        }
        
        // Initialize the Gang of Three actors.  Will need a start message.
        try {
            log.info("DdsActorController: Initializing Gang of Three actors...");
            gangOfThreeRouter = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("gof3DiscoveryRouter"), "discovery-Gof3-registration");
            startList.add(gangOfThreeRouter);
            log.info("DdsActorController:... Gang of Three actors initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize Gang of Three actors.", ex);
        }

        // Initialize the A-GOLE actors.  Will need a start message.
        try {
            log.info("DdsActorController: Initializing A-GOLE actors...");
            agoleRouter = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props("agoleDiscoveryRouter"), "discovery-AGOLE-registration");
            startList.add(agoleRouter);
            log.info("DdsActorController:... A-GOLE actors initialized.");
        } catch (Exception ex) {
            log.error("DdsActorController: Failed to initialize A-GOLE actors.", ex);
        }
    }
    
    public ActorSystem getActorSystem() {
        return ddsActorSystem.getActorSystem();
    }
    
    public DiscoveryConfiguration getConfigReader() {
        return configReader;
    }
    
    public Cancellable scheduleNotification(Object message, long delay) {
        Cancellable scheduleOnce = ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(delay, TimeUnit.SECONDS), notificationRouter, message, ddsActorSystem.getActorSystem().dispatcher(), null);
        return scheduleOnce;
    }
    
    public void sendNotification(Object message) {
        notificationRouter.tell(message, null);
    }
    
    public void start() {
        log.info("DdsActorController: Starting discovery process...");
        StartMsg msg = new StartMsg();
        for (ActorRef ref : startList) {
            ref.tell(msg, null);
        }
    }
    public void shutdown() {
        log.info("DdsActorController: Shutting down actor system...");
        ddsActorSystem.getActorSystem().shutdown();
    }
}
