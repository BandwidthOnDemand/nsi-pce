package net.es.nsi.pce.schema;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 * 
 * @author hacksaw
 */
public class PathApiParser {
    // Get a logger just in case we encounter a problem.
    private static final Logger log = LoggerFactory.getLogger(PathApiParser.class);
    
    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContextAPI = null;
        
    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private PathApiParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContextAPI = JAXBContext.newInstance("net.es.nsi.pce.path.jaxb", net.es.nsi.pce.path.jaxb.ObjectFactory.class.getClassLoader());
        }
        catch (JAXBException jaxb) {
            log.error("NmlParser: Failed to load JAXB instance", jaxb);
        }
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class PceApiParserHolder {
        public static final PathApiParser INSTANCE = new PathApiParser();
    }

    /**
     * Returns an instance of this singleton class.
     * 
     * @return An NmlParser object of the NSAType.
     */
    public static PathApiParser getInstance() {
            return PceApiParserHolder.INSTANCE;
    }

    public void init() {
        log.debug("PathApiParser: initializing...");
    }

    /**
     * Parse an PCE Error object from the specified string.
     * 
     * @param xml String containing the XML formated FindPathErrorType object.
     * @return A JAXB compiled FindPathErrorType object.
     * @throws JAXBException If the XML contained in the string is not valid.
     * @throws JAXBException If the XML is not well formed.
     */
    public Object stringToJaxb(String xml) throws JAXBException {
        // Make sure we initialized properly.
        if (jaxbContextAPI == null) {
            throw new JAXBException("jaxbFromString: Failed to load JAXB PCE API instance");
        }
        
        @SuppressWarnings(value = "unchecked")
        JAXBElement<?> jaxbElement;
        try (StringReader reader = new StringReader(xml)) {
            jaxbElement = (JAXBElement<?>) jaxbContextAPI.createUnmarshaller().unmarshal(reader);
        }
        
        // Return the NSAType object.
        return jaxbElement;
    }

	public String jaxbToString(Class<?> xmlClass, Object xmlObject) {

            // Make sure we are given the correct input.
            if (xmlClass == null || xmlObject == null) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);
            
            return jaxbToString(jaxbElement);
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
            Marshaller jaxbMarshaller = jaxbContextAPI.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(jaxbElement, writer);
        } catch (Exception e) {             
            // Something went wrong so get out of here.
            log.error("NsiParser.jaxbToString: Error marshalling object " +
                jaxbElement.getClass() + ": " + e.getMessage());
            
            return null;
        }
        finally {
            try { writer.close(); } catch (IOException ex) {}
        }

        // Return the XML string.
        return writer.toString();
	}
}