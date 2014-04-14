package net.es.nsi.pce.discovery;

import java.io.File;
import java.net.URLEncoder;
import javax.ws.rs.client.Entity;
import net.es.nsi.pce.jersey.RestServer;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.client.TestServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.discovery.jaxb.DocumentEventType;
import net.es.nsi.pce.discovery.jaxb.DocumentListType;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.FilterCriteriaType;
import net.es.nsi.pce.discovery.jaxb.FilterType;
import net.es.nsi.pce.discovery.jaxb.NotificationListType;
import net.es.nsi.pce.discovery.jaxb.NotificationType;
import net.es.nsi.pce.discovery.jaxb.NsaType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.jaxb.SubscriptionRequestType;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.provider.DiscoveryParser;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.schema.XmlUtilities;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.test.TestProperties;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DiscoveryTest extends JerseyTest {

    private final static HttpConfig testServer = new HttpConfig() {
        {
            setUrl("http://localhost:9801/");
            setPackageName("net.es.nsi.pce.client");
        }
    };
    
    private final static String CONFIG_DIR = "src/test/resources/config/";
    private final static String DOCUMENT_DIR = "src/test/resources/documents/";
    private final static String callbackURL = testServer.getUrl() + "discovery/callback";
    private final static ObjectFactory factory = new ObjectFactory();
    final WebTarget discovery = target().path("discovery");

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        // Configure the local test client callback server.
        TestServer.INSTANCE.start(testServer);
        
        // Configure test instance of PCE server.
        try {
            ConfigurationManager.INSTANCE.initialize(CONFIG_DIR);
        } catch (Exception ex) {
            System.err.println("configure(): Could not initialize test environment.");
            ex.printStackTrace();
            ConfigurationManager.INSTANCE.shutdown();
            fail("configure(): Could not initialize test environment.");
        }
        Application app = new Application();
        app.getProperties();
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().getPackageName());
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }

    /**
     * Load the Discovery Service with a default set of documents.
     * 
     * @throws Exception 
     */
    @Test
    public void aLoadDocuments() throws Exception {
        System.out.println("************************** Running aLoadDocuments test ********************************");
        File folder = null;
        try {
            folder = new File(DOCUMENT_DIR);
        }
        catch (NullPointerException ex) {
            System.err.println("Failed to load directory " + DOCUMENT_DIR + ", " + ex.getMessage());
            throw ex;
        }
        
        // We will grab all XML files from the target directory. 
        File[] listOfFiles = folder.listFiles(); 
        
        // For each document file in the document directory load into discovery service.
        String file;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                file = listOfFiles[i].getAbsolutePath();
                if (file.endsWith(".xml") || file.endsWith(".xml")) {
                    DocumentType document = DiscoveryParser.getInstance().readDocument(file);
                    JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
                    Response response = discovery.path("documents").request(MediaType.APPLICATION_XML).post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {}, MediaType.APPLICATION_XML));
                    if (Response.Status.CREATED.getStatusCode() != response.getStatus() &&
                            Response.Status.CONFLICT.getStatusCode() != response.getStatus()) {
                        fail();
                    }
                }
            }
        }
    }
    
    /**
     * A simple get on the ping URL.
     */
    @Test
    public void bPing() {
        // Simple ping to determine if interface is available.
        Response response = discovery.path("ping").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    /**
     * Queries the default set of documents.
     * 
     * @throws Exception 
     */
    @Test
    public void cDocumentsFull() throws Exception {
        System.out.println("************************** Running cDocumentsFull test ********************************");
        // Get a list of all documents with full contents.
        Response response = discovery.path("documents").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {});
        DocumentListType chunk;
        DocumentListType documents = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            documents = chunk;
        }
        assertNotNull(documents);
        
        for (DocumentType document : documents.getDocument()) {
            System.out.println("cDocumentsFull: " + document.getNsa() + ", " + document.getType() + ", " + document.getId() + ", href=" + document.getHref());
            assertFalse(document.getContent().getAny().isEmpty());
            
            response = discovery.path(document.getHref()).request(MediaType.APPLICATION_XML).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            DocumentType doc = response.readEntity(DocumentType.class);
            assertNotNull(doc);
            assertFalse(doc.getContent().getAny().isEmpty());
            
            // Do a search using the NSA and Type from previous result.
            response = discovery.path("documents")
                    .path(URLEncoder.encode(document.getNsa().trim(), "UTF-8"))
                    .path(URLEncoder.encode(document.getType().trim(), "UTF-8"))
                    .request(MediaType.APPLICATION_XML).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            DocumentListType docList = response.readEntity(DocumentListType.class);
            boolean found = false;
            for (DocumentType docListItem : docList.getDocument()) {
                if (document.getNsa().equalsIgnoreCase(docListItem.getNsa()) &&
                        document.getType().equalsIgnoreCase(docListItem.getType()) &&
                        document.getId().equalsIgnoreCase(docListItem.getId())) {
                    found = true;
                }
            }
            
            assertTrue(found);
        }     
    }

    /**
     * Queries the default set of documents.
     * 
     * @throws Exception 
     */
    @Test
    public void dDocumentsSummary() throws Exception {
        System.out.println("************************** Running dDocumentsSummary test ********************************");
        // Get a list of all documents with summary contents.
        Response response = discovery.path("documents").queryParam("summary", "true").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        DocumentListType documents = response.readEntity(DocumentListType.class);
        assertNotNull(documents);

        for (DocumentType document : documents.getDocument()) {
            System.out.println("dDocumentsSummary: " + document.getNsa() + ", " + document.getType() + ", " + document.getId() + ", href=" + document.getHref());
            assertNull(document.getContent());
            
            // Read the direct href and get summary contents.
            response = discovery.path(document.getHref()).queryParam("summary", "true").request(MediaType.APPLICATION_XML).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            DocumentType doc = response.readEntity(DocumentType.class);
            assertNotNull(doc);
            assertNull(doc.getContent());
        }       
    }

    @Test
    public void eDocumentNotFound() throws Exception {
        System.out.println("************************** Running eDocumentNotFound test ********************************");
        // We want a NOT_FOUND for a nonexistent nsa resource on document path.
        Response response = discovery.path("documents").path("invalidNsaValue").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        
        // We want an empty result set for an invalid type on document path.
        response = discovery.path("documents").path("urn:ogf:network:czechlight.cesnet.cz:2013:nsa").path("invalidDocumentType").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        DocumentListType documents = response.readEntity(DocumentListType.class);
        assertNotNull(documents);
        assertTrue(documents.getDocument().isEmpty());
    }
    
    @Test
    public void fLocalDocuments() throws Exception {
        System.out.println("************************** Running fLocalDocuments test ********************************");
        // Get a list of all documents with full contents.
        Response response = discovery.path("local").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {});
        DocumentListType chunk;
        DocumentListType documents = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            documents = chunk;
        }
        assertNotNull(documents);
        
        for (DocumentType document : documents.getDocument()) {
            assertEquals(document.getNsa(), DiscoveryConfiguration.getInstance().getNsaId());
            
            response = discovery.path("local").path(URLEncoder.encode(document.getType(), "UTF-8")).request(MediaType.APPLICATION_XML).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            DocumentListType docs = response.readEntity(DocumentListType.class);
            assertNotNull(docs);
            
            for (DocumentType d : docs.getDocument()) {
                assertEquals(document.getType(), d.getType());
            }
        }
    }

    @Test
    public void fUpdateDocuments() throws Exception {
        System.out.println("************************** Running fUpdateDocuments test ********************************");
        File folder = null;
        try {
            folder = new File(DOCUMENT_DIR);
        }
        catch (NullPointerException ex) {
            System.err.println("Failed to load directory " + DOCUMENT_DIR + ", " + ex.getMessage());
            throw ex;
        }
        
        // We will grab all XML files from the target directory. 
        File[] listOfFiles = folder.listFiles(); 
        
        // For each document file in the document directory load into discovery service.
        String file;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                file = listOfFiles[i].getAbsolutePath();
                if (file.endsWith(".xml") || file.endsWith(".xml")) {
                    DocumentType document = DiscoveryParser.getInstance().readDocument(file);
                    XMLGregorianCalendar currentTime = XmlUtilities.xmlGregorianCalendar();
                    XMLGregorianCalendar future = XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 360000);
                    document.setExpires(future);
                    document.setVersion(currentTime);
                    for (Object obj : document.getAny()) {
                        if (obj instanceof JAXBElement<?>) {
                            JAXBElement<?> jaxb = (JAXBElement<?>) obj;
                            if (jaxb.getValue() instanceof NsaType) {
                                NsaType nsa = (NsaType) jaxb.getValue();
                                nsa.setVersion(currentTime);
                                nsa.setExpires(future);
                            }
                        }
                    }
                    
                    System.out.println("fUpdateDocuments: updating document " + document.getId());
                    
                    JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
                    Response response = discovery.path("documents")
                            .path(URLEncoder.encode(document.getNsa().trim(), "UTF-8"))
                            .path(URLEncoder.encode(document.getType().trim(), "UTF-8"))
                            .path(URLEncoder.encode(document.getId().trim(), "UTF-8"))
                            .request(MediaType.APPLICATION_XML)
                            .put(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {}, MediaType.APPLICATION_XML));
                    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());                    
                }
            }
        }
        System.out.println("************************** Done fUpdateDocuments test ********************************");
    }

    @Test
    public void gAddNotification() throws Exception {
        System.out.println("************************** Running gAddNotification test ********************************");
        // Register for ALL document event types.
        SubscriptionRequestType subscription = factory.createSubscriptionRequestType();
        subscription.setRequesterId("urn:ogf:network:es.net:2013:nsa");
        subscription.setCallback(callbackURL);
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        subscription.setFilter(filter);
        JAXBElement<SubscriptionRequestType> jaxbRequest = factory.createSubscriptionRequest(subscription);
        Response response = discovery.path("subscriptions").request(MediaType.APPLICATION_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxbRequest) {}, MediaType.APPLICATION_XML));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        SubscriptionType result = response.readEntity(SubscriptionType.class);
        response = discovery.path(result.getHref()).request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        response = discovery.path(result.getHref()).request(MediaType.APPLICATION_XML).delete();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void hNotification() throws Exception {
        System.out.println("************************** Running hNotification test ********************************");
        // Register for ALL document event types.
        SubscriptionRequestType subscription = factory.createSubscriptionRequestType();
        subscription.setRequesterId("urn:ogf:network:es.net:2013:nsa");
        subscription.setCallback(callbackURL);
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        subscription.setFilter(filter);
        JAXBElement<SubscriptionRequestType> jaxbRequest = factory.createSubscriptionRequest(subscription);
        Response response = discovery.path("subscriptions").request(MediaType.APPLICATION_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxbRequest) {}, MediaType.APPLICATION_XML));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        SubscriptionType result = response.readEntity(SubscriptionType.class);
        response = discovery.path(result.getHref()).request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        // Now we wait for the initial notifications to arrive.
        int count = 0;
        NotificationListType notifications = TestServer.INSTANCE.peekDiscoveryNotification();
        while(notifications == null && count < 30) {
            count++;
            Thread.sleep(1000);
            notifications = TestServer.INSTANCE.peekDiscoveryNotification();
        }
        
        assertNotNull(notifications);
        notifications = TestServer.INSTANCE.pollDiscoveryNotification();
        while (notifications != null) {
            System.out.println("Notification: providerId=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId() );
            for (NotificationType notification : notifications.getNotification()) {
                System.out.println("Notification: event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
            }
            notifications = TestServer.INSTANCE.pollDiscoveryNotification();
        }
        
        // Now send a document update.
        fUpdateDocuments();
        
        // Now we wait for the update notifications to arrive.
        count = 0;
        notifications = TestServer.INSTANCE.peekDiscoveryNotification();
        while(notifications == null && count < 30) {
            count++;
            Thread.sleep(1000);
            notifications = TestServer.INSTANCE.peekDiscoveryNotification();
        }
        
        assertNotNull(notifications);
        notifications = TestServer.INSTANCE.pollDiscoveryNotification();
        while (notifications != null) {
            System.out.println("Notification: providerId=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId() );
            for (NotificationType notification : notifications.getNotification()) {
                System.out.println("Notification: event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
            }
            notifications = TestServer.INSTANCE.pollDiscoveryNotification();
        }
    }
    
    @Test
    public void iReadEachDocumentType() throws Exception {
        readDocumentType(NsiConstants.NSI_DOC_TYPE_NSA_V1);
        readDocumentType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
    }
    
    public void readDocumentType(String type) throws Exception {
        String encode = URLEncoder.encode(type, "UTF-8");
        Response response = discovery.path("documents").queryParam("type", encode).queryParam("summary", true).request(NsiConstants.NSI_DDS_V1_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        DocumentListType documents = null;
        try (final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {})) {
            DocumentListType chunk;
            while ((chunk = chunkedInput.read()) != null) {
                documents = chunk;
            }
        }
        
        assertNotNull(documents);
        assertNotNull(documents.getDocument());
        assertFalse(documents.getDocument().isEmpty());
        
        for (DocumentType document : documents.getDocument()) {
            System.out.println("readDocumentType: reaading document " + document.getId());
            response = discovery.path(document.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }
}
