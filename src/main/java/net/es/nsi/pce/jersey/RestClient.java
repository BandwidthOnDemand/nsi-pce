package net.es.nsi.pce.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RestClient {
    private final Client client;

    public RestClient() {
        ClientConfig clientConfig = new ClientConfig();
        configureClient(clientConfig);
        ClientBuilder builder = ClientBuilder.newBuilder().withConfig(clientConfig);
        PemSslContext.fromSystemProperties().ifPresent(builder::sslContext);
        client = builder.build();
    }

    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }

    public static void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        clientConfig.register(FollowRedirectFilter.class);
        // FINE reaches Logback as DEBUG, so request and payload dumps only show at DEBUG.
        clientConfig.register(new LoggingFeature(java.util.logging.Logger.getGlobal(), Level.FINE, LoggingFeature.Verbosity.PAYLOAD_ANY, null));
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
        private final static Logger log = LoggerFactory.getLogger(FollowRedirectFilter.class);

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
        {
            if (requestContext == null || responseContext == null || responseContext.getStatus() != Response.Status.FOUND.getStatusCode()) {
               return;
            }

            log.debug("Processing redirect for " + requestContext.getMethod() + " " + requestContext.getUri().toASCIIString() + " to " + responseContext.getLocation().toASCIIString());

            Client inClient = requestContext.getClient();
            Object entity = requestContext.getEntity();
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            String method = requestContext.getMethod();
            Response resp;
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod(), Entity.entity(new GenericEntity<JAXBElement<?>>((JAXBElement<?>)entity) {}, NsiConstants.NSI_DDS_V1_XML));
            }
            else {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod());
            }

            responseContext.setEntityStream((InputStream) resp.getEntity());
            responseContext.setStatusInfo(resp.getStatusInfo());
            responseContext.setStatus(resp.getStatus());
            responseContext.getHeaders().putAll(resp.getStringHeaders());

            log.debug("Processing redirect with result " + resp.getStatus());
        }
    }
}
