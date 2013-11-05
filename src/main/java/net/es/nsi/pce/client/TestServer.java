package net.es.nsi.pce.client;

import net.es.nsi.pce.jersey.JsonMoxyConfigurationContextResolver;
import java.net.URI;
import net.es.nsi.pce.config.http.HttpConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;

public enum TestServer {
    INSTANCE;
    
    private static HttpServer server = null;
    
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