package net.es.nsi.pce.config.topo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.topology.jaxb.NSAType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 * 
 * @author hacksaw
 */
public class NmlParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContext = null;
        
    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private NmlParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContext = JAXBContext.newInstance(NSAType.class);
        }
        catch (JAXBException jaxb) {
            log.error("NmlParser: Failed to load JAXB instance", jaxb);
        }
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class NmlParserHolder {
        public static final NmlParser INSTANCE = new NmlParser();
    }

    /**
     * Returns an instance of this singleton class.
     * 
     * @return An NmlParser object of the NSAType.
     */
    public static NmlParser getInstance() {
            return NmlParserHolder.INSTANCE;
    }
    
    /**
     * Parse an NML NSA object from the specified file.
     * 
     * @param file File containing the XML formated NSA object.
     * @return A JAXB compiled NSAType object.
     * @throws JAXBException If the XML contained in the file is not valid.
     * @throws FileNotFoundException If the specified file was not found.
     */
    public NSAType parseNSA(String file) throws JAXBException, FileNotFoundException {
        // Make sure we initialized properly.
        if (jaxbContext == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB instance");
        }
        
        // Parse the specified file.
        @SuppressWarnings("unchecked")
        JAXBElement<NSAType> nsaElement = (JAXBElement<NSAType>) jaxbContext.createUnmarshaller().unmarshal(new BufferedInputStream(new FileInputStream(file)));
        
        // Return the NSAType object.
        return nsaElement.getValue();
    }
}