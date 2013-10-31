package net.es.nsi.pce.schema;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.topology.jaxb.ConfigurationType;
import net.es.nsi.pce.topology.jaxb.NSAType;
import net.es.nsi.pce.topology.jaxb.TopologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 * 
 * @author hacksaw
 */
public class XmlParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContextNSA = null;
    private static JAXBContext jaxbContextTopology = null;
    private static JAXBContext jaxbContextTopologyConfiguration = null;
        
    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private XmlParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContextNSA = JAXBContext.newInstance(NSAType.class);
            jaxbContextTopology = JAXBContext.newInstance(TopologyType.class);
            jaxbContextTopologyConfiguration = JAXBContext.newInstance(ConfigurationType.class);
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
        public static final XmlParser INSTANCE = new XmlParser();
    }

    /**
     * Returns an instance of this singleton class.
     * 
     * @return An NmlParser object of the NSAType.
     */
    public static XmlParser getInstance() {
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
        if (jaxbContextNSA == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB instance");
        }
        
        // Parse the specified file.
        @SuppressWarnings("unchecked")
        JAXBElement<NSAType> nsaElement = (JAXBElement<NSAType>) jaxbContextNSA.createUnmarshaller().unmarshal(new BufferedInputStream(new FileInputStream(file)));
        
        // Return the NSAType object.
        return nsaElement.getValue();
    }
    
    /**
     * Parse an NML Topology object from the specified string.
     * 
     * @param xml String containing the XML formated Topology object.
     * @return A JAXB compiled TopologyType object.
     * @throws JAXBException If the XML contained in the string is not valid.
     * @throws JAXBException If the XML is not well formed.
     */
    public TopologyType parseTopologyFromString(String xml) throws JAXBException {
        // Make sure we initialized properly.
        if (jaxbContextTopology == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB Topology instance");
        }
        
        // Parse the specified XML string.
        StringReader reader = new StringReader(xml);
        
        @SuppressWarnings("unchecked")
        JAXBElement<TopologyType> topologyElement = (JAXBElement<TopologyType>) jaxbContextTopology.createUnmarshaller().unmarshal(reader);
        
        // Return the NSAType object.
        return topologyElement.getValue();
    }
    
    /**
     * Parse an NML NSA object from the specified string.
     * 
     * @param xml String containing the XML formated NSA object.
     * @return A JAXB compiled NSAType object.
     * @throws JAXBException If the XML contained in the string is not valid.
     * @throws JAXBException If the XML is not well formed.
     */
    public NSAType parseNsaFromString(String xml) throws JAXBException {
        // Make sure we initialized properly.
        if (jaxbContextNSA == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB NSA instance");
        }
        
        // Parse the specified XML string.
        StringReader reader = new StringReader(xml);
        
        @SuppressWarnings("unchecked")
        JAXBElement<NSAType> nsaElement = (JAXBElement<NSAType>) jaxbContextTopology.createUnmarshaller().unmarshal(reader);
        
        // Return the NSAType object.
        return nsaElement.getValue();
    }
    
    /**
     * Parse an topology configuration file from the specified file.
     * 
     * @param file File containing the XML formated topology configuration.
     * @return A JAXB compiled ConfigurationType object.
     * @throws JAXBException If the XML contained in the file is not valid.
     * @throws FileNotFoundException If the specified file was not found.
     */
    public ConfigurationType parseTopologyConfiguration(String file) throws JAXBException, FileNotFoundException {
        // Make sure we initialized properly.
        if (jaxbContextTopologyConfiguration == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB instance");
        }
        
        // Parse the specified file.
        @SuppressWarnings("unchecked")
        JAXBElement<ConfigurationType> configurationElement = (JAXBElement<ConfigurationType>) jaxbContextNSA.createUnmarshaller().unmarshal(new BufferedInputStream(new FileInputStream(file)));
        
        // Return the NSAType object.
        return configurationElement.getValue();
    }
}