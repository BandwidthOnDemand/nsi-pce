package net.es.nsi.pce.jersey;

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
    
    public static void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
        clientConfig.property(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, Utilities.getNameSpace());
        clientConfig.property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        clientConfig.property(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
    }
}
