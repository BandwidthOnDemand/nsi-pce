package net.es.nsi.pce.jersey;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RestClient {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Client client;

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
        clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, true);
    }

    public Client get() {
        return client;
    }

    public void close() {
        client.close();
    }
}
