package net.es.nsi.pce.jersey;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;
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
        clientConfig.register(FollowRedirectFilter.class);
        clientConfig.property(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, Utilities.getNameSpace());
        clientConfig.property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        clientConfig.property(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
    }

    public Client get() {
        return client;
    }

    public void close() {
        client.close();
    }

    private static class FollowRedirectFilter implements ClientResponseFilter
    {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
        {
            if (requestContext == null || responseContext == null || responseContext.getStatus() != Response.Status.FOUND.getStatusCode()) {
               return;
            }

            Response resp = requestContext.getClient().target(responseContext.getLocation()).request().method(requestContext.getMethod());
            responseContext.setEntityStream((InputStream) resp.getEntity());
            responseContext.setStatusInfo(resp.getStatusInfo());
            responseContext.setStatus(resp.getStatus());
        }
    }
}
