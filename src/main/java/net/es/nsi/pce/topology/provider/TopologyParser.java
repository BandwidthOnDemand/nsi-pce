
package net.es.nsi.pce.topology.provider;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.es.nsi.pce.jaxb.topology.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 *
 * @author hacksaw
 */
public class TopologyParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContext = null;

    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private TopologyParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContext = JAXBContext.newInstance("net.es.nsi.pce.jaxb.topology", net.es.nsi.pce.jaxb.topology.ObjectFactory.class.getClassLoader());
        }
        catch (JAXBException jaxb) {
            log.error("NmlParser: Failed to load JAXB instance", jaxb);
        }
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class TopologyParserHolder {
        public static final TopologyParser INSTANCE = new TopologyParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static TopologyParser getInstance() {
            return TopologyParserHolder.INSTANCE;
    }

    public void init() {
        log.debug("TopologyParser: initializing...");
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