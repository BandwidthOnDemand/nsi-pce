package net.es.nsi.pce.schema;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.config.TopologyConfigurationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 *
 * @author hacksaw
 */
public class TopologyConfigurationParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());

    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContext = null;

    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private TopologyConfigurationParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContext = JAXBContext.newInstance("net.es.nsi.pce.jaxb.config", net.es.nsi.pce.jaxb.config.ObjectFactory.class.getClassLoader());
        }
        catch (JAXBException jaxb) {
            log.error("NmlParser: Failed to load JAXB instance", jaxb);
        }
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ConfigParserHolder {
        public static final TopologyConfigurationParser INSTANCE = new TopologyConfigurationParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static TopologyConfigurationParser getInstance() {
            return ConfigParserHolder.INSTANCE;
    }

    public void init() {
        log.debug("TopologyConfigurationParser: initializing...");
    }

    /**
     * Parse an topology configuration file from the specified file.
     *
     * @param file File containing the XML formated topology configuration.
     * @return A JAXB compiled TopologyConfigurationType object.
     * @throws JAXBException If the XML contained in the file is not valid.
     * @throws FileNotFoundException If the specified file was not found.
     */
    @SuppressWarnings({"unchecked", "unchecked"})
    public TopologyConfigurationType parse(String file) throws JAXBException, IOException {
        // Make sure we initialized properly.
        if (jaxbContext == null) {
            throw new JAXBException("parseTopologyConfiguration: Failed to load JAXB instance");
        }

        // Parse the specified file.
        JAXBElement<TopologyConfigurationType> configurationElement;
        try {
            Object result;
            try (FileInputStream fileInputStream = new FileInputStream(file); BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                result = jaxbContext.createUnmarshaller().unmarshal(bufferedInputStream);
            }

            if (result instanceof JAXBElement<?> && ((JAXBElement<?>) result).getValue() instanceof TopologyConfigurationType) {
                configurationElement = (JAXBElement<TopologyConfigurationType>) result;
            }
            else {
                throw new IllegalArgumentException("Expected TopologyConfigurationType from " + file);
            }
        }
        catch (JAXBException | IOException ex) {
            log.error("parseTopologyConfiguration: unmarshall error from file " + file, ex);
            throw ex;
        }
        // Return the NSAType object.
        return configurationElement.getValue();
    }
}