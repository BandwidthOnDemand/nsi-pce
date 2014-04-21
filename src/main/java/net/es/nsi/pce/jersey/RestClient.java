package net.es.nsi.pce.jersey;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;

/**
 *
 * @author hacksaw
 */
public class RestClient {
    private Client client;
    
    public RestClient() {
        ClientConfig clientConfig = new ClientConfig();
        configureClient(clientConfig);
        client = ClientBuilder.newClient(clientConfig);
    }
    
    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }
    
    public static void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
        clientConfig.property(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, Utilities.getNameSpace());
        clientConfig.property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        clientConfig.property(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
    }
    
    public Client get() {
        return client;
    }

    public void close() {
        client.close();
        client = null;
    }
}
