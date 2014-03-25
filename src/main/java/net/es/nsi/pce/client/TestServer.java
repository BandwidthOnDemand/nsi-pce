package net.es.nsi.pce.client;


import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.es.nsi.pce.path.jaxb.FindPathResponseType;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.discovery.jaxb.NotificationListType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import net.es.nsi.pce.jersey.JsonMoxyConfigurationContextResolver;
import org.glassfish.jersey.server.ResourceConfig;

public enum TestServer {
    INSTANCE;
    
    private static HttpServer server = null;
    private static FindPathResponseType findPathResponse = null;
    private static ConcurrentLinkedQueue<NotificationListType> notificationQueue = new ConcurrentLinkedQueue<>();;

    public boolean pushDiscoveryNotification(NotificationListType notify) {
        return notificationQueue.add(notify);
    }
    
    public NotificationListType popDiscoveryNotification() {
        return notificationQueue.remove();
    }
    
    public NotificationListType peekDiscoveryNotification() {
        return notificationQueue.peek();
    }
    
    public NotificationListType pollDiscoveryNotification() {
        return notificationQueue.poll();
    }

    /**
     * @return the findPathResponse
     */
    public FindPathResponseType getFindPathResponse() {
        return findPathResponse;
    }

    /**
     * @param aFindPathResponse the findPathResponse to set
     */
    public void setFindPathResponse(FindPathResponseType aFindPathResponse) {
        findPathResponse = aFindPathResponse;
    }
    
    public void start(HttpConfig config) throws IllegalStateException {
        
        synchronized(this) {
            if (server == null) {
                server = GrizzlyHttpServerFactory.createHttpServer(URI.create(config.getUrl()), getConfig(config.getPackageName()));
            }
        }
    }

    public void stop() throws IllegalStateException {
        synchronized(this) {
            if (server != null) {
                server.stop();
                server = null;
            }
        }
    }
    
    public ResourceConfig getConfig(String packageName) throws IllegalStateException {

        return new ResourceConfig()
                .packages(packageName)
                .register(new MoxyXmlFeature())
                .register(new MoxyJsonFeature())
                .registerInstances(new JsonMoxyConfigurationContextResolver());
    }    
}