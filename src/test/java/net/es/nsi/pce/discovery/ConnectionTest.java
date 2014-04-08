/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.discovery.jaxb.DocumentListType;
import net.es.nsi.pce.discovery.jaxb.NmlTopologyType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.jersey.JsonMoxyConfigurationContextResolver;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.Utilities;
import net.es.nsi.pce.schema.NsiConstants;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class ConnectionTest extends JerseyTest {
    private final static ObjectFactory factory = new ObjectFactory();
    private final static String url = "https://bod.acc.dlp.surfnet.nl/nsi-topology";
    
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        Application app = new Application();
        app.getProperties();
        return getConfig();
    }
    
    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
        clientConfig.property(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, Utilities.getNameSpace());
        clientConfig.property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        clientConfig.property(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
    }
    
    public static ResourceConfig getConfig() {
        return new ResourceConfig()
                .register(new MoxyXmlFeature())
                .register(new MoxyJsonFeature())
                .registerInstances(new JsonMoxyConfigurationContextResolver());
    }

    @Test
    public void getTopology() throws Exception {
        // Get a list of all documents with full contents.
        ClientConfig clientConfig = new ClientConfig();
        configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget nmlTarget = client.target(url);
        Response response = nmlTarget.request("*/*").get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        NmlTopologyType documents = response.readEntity(NmlTopologyType.class);
        /*
        final ChunkedInput<NmlTopologyType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<NmlTopologyType>>() {});
        NmlTopologyType chunk;
        NmlTopologyType documents = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            documents = chunk;
        }*/
        assertNotNull(documents);
    }
}
