package net.es.nsi.pce.discovery.provider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.es.nsi.pce.discovery.jaxb.DiscoveryConfigurationType;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 * 
 * @author hacksaw
 */
public class DiscoveryParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    
    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContext = null;
        
    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private DiscoveryParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContext = JAXBContext.newInstance("net.es.nsi.pce.discovery.jaxb", net.es.nsi.pce.discovery.jaxb.ObjectFactory.class.getClassLoader());
        }
        catch (JAXBException jaxb) {
            log.error("NmlParser: Failed to load JAXB instance", jaxb);
        }
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class DiscoveryParserHolder {
        public static final DiscoveryParser INSTANCE = new DiscoveryParser();
    }

    /**
     * Returns an instance of this singleton class.
     * 
     * @return An NmlParser object of the NSAType.
     */
    public static DiscoveryParser getInstance() {
            return DiscoveryParserHolder.INSTANCE;
    }
    
    public void init() {
        log.debug("DiscoveryParser: initializing...");
    }
    
    /**
     * Parse an topology configuration file from the specified file.
     * 
     * @param file File containing the XML formated topology configuration.
     * @return A JAXB compiled ConfigurationType object.
     * @throws JAXBException If the XML contained in the file is not valid.
     * @throws FileNotFoundException If the specified file was not found.
     */
    @SuppressWarnings({"unchecked", "unchecked"})
    public DiscoveryConfigurationType parse(String file) throws JAXBException, FileNotFoundException {
        // Make sure we initialized properly.
        if (jaxbContext == null) {
            throw new JAXBException("parseTopologyConfiguration: Failed to load JAXB instance");
        }
        
        // Parse the specified file.
        JAXBElement<DiscoveryConfigurationType> configurationElement;
        try {
            Object result = jaxbContext.createUnmarshaller().unmarshal(new BufferedInputStream(new FileInputStream(file)));
            if (result instanceof JAXBElement<?> && ((JAXBElement<?>) result).getValue() instanceof DiscoveryConfigurationType) {
                configurationElement = (JAXBElement<DiscoveryConfigurationType>) result;
            }
            else {
                throw new IllegalArgumentException("Expected DiscoveryConfigurationType from " + file);
            }
        }
        catch (JAXBException | FileNotFoundException ex) {
            log.error("parse: unmarshall error from file " + file, ex);
            throw ex;
        }
        // Return the NSAType object.
        return configurationElement.getValue();
    }
    
    @SuppressWarnings("unchecked")
    public DocumentType readDocument(String file) throws JAXBException, FileNotFoundException {
        // Make sure we initialized properly.
        if (jaxbContext == null) {
            throw new JAXBException("readDocument: Failed to load JAXB instance");
        }
        
        // Parse the specified file.
        JAXBElement<DocumentType> document;
        try {
            Object result = jaxbContext.createUnmarshaller().unmarshal(new BufferedInputStream(new FileInputStream(file)));
            if (result instanceof JAXBElement<?> && ((JAXBElement<?>) result).getValue() instanceof DocumentType) {
                document = (JAXBElement<DocumentType>) result;
            }
            else {
                throw new IllegalArgumentException("Expected DocumentType from " + file);
            }
        }
        catch (JAXBException | FileNotFoundException ex) {
            log.error("parse: unmarshall error from file " + file, ex);
            throw ex;
        }
        // Return the NSAType object.
        return document.getValue();
    }
    
    @SuppressWarnings("unchecked")
    public void writeDocument(String file, DocumentType document) throws JAXBException, IOException {
        // Make sure we initialized properly.
        if (jaxbContext == null) {
            throw new JAXBException("writeDocument: Failed to load JAXB instance");
        }
        
        // Parse the specified file.
        JAXBElement<DocumentType> element = factory.createDocument(document);

        File fd = new File(file);
        try (FileOutputStream fs = new FileOutputStream(file)) {
            if (!fd.exists()) {
                fd.createNewFile();
            }

            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(element, fs);

            fs.flush();
        }
    }
    
    public Object stringToJaxb(String xml) throws JAXBException {
        // Make sure we initialized properly.
        if (jaxbContext == null) {
            throw new JAXBException("jaxbFromString: Failed to load JAXB PCE API instance");
        }
        
        // Parse the specified XML string.
        StringReader reader = new StringReader(xml);
        
        @SuppressWarnings("unchecked")
        JAXBElement<?> jaxbElement = (JAXBElement<?>) jaxbContext.createUnmarshaller().unmarshal(reader);
        
        // Return the NSAType object.
        return jaxbElement;
    }
    
    public String jaxbToString(JAXBElement<?> jaxbElement) {

        // Make sure we are given the correct input.
        if (jaxbElement == null) {
            return null;
        }

        // We will write the XML encoding into a string.
        StringWriter writer = new StringWriter();

        try {
            // Marshal the object.
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(jaxbElement, writer);
        } catch (Exception e) {             
            // Something went wrong so get out of here.
            log.error("jaxbToString: Error marshalling object " + jaxbElement.getClass() + ": " + e.getMessage());
            return null;
        }

        // Return the XML string.
        return writer.toString();
	}
}