package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.schema.XmlParser;
import java.io.File;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.topology.jaxb.NSAType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A file based provider that reads XML formatted NML topology files and
 * creates simple network objects used to later build NSI topology.  Each
 * instance of the class models one NSA document in NML.
 * 
 * @author hacksaw
 */
@Component
public class FileTopologyReader extends NmlTopologyReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * Default class constructor.
     */
    public FileTopologyReader() {}
    
    /**
     * Class constructor takes the filename from which to load the
     * NSA's associated NML topology.
     * 
     * @param target Location of the NSA's XML based NML topology.
     */
    public FileTopologyReader(String target) {
        this.setTarget(target);
    }
    
    protected boolean isFileUpdated() {
        File file = new File(this.getTarget());
        
        long currentModified = file.lastModified();
        long lastModifiled = getLastModified();
        
        log.debug("isFileUpdated: filename = " + getTarget()  + ", last = " + lastModifiled + ", current = " + currentModified);
        
        // Did we have an update? 
        if (currentModified > lastModifiled) {
            this.setLastModified(currentModified);
            return true;
        }

        // No, file is not updated.
        return false;
    }
    
    /**
     * Read the NML topology from target file.
     * 
     * @return The JAXB NSA element from the NML topology.
     */
    @Override
    public NSAType readNsaTopology() throws Exception {
        // Check to see if file associated with this topology has changed.
        File configFile = new File(this.getTarget()); 
        if (!configFile.exists()) {
            throw new FileNotFoundException("Topology file does not exist: " + configFile);
        }
        
        String ap = configFile.getAbsolutePath();
        log.info("Processing NML topology file " + ap);

        if (!isFileUpdated()) {
            log.info("No change, file already loaded: " + ap);
            return null;
        }
        
        // Looks like we have a change and need to process.
        log.debug("File change detected, loading " + ap);

        NSAType newNsa;
        try {
            newNsa = XmlParser.getInstance().parseNSAFromFile(ap);
            log.info("Loaded topology for NSA " + newNsa.getId());
        }
        catch (JAXBException | FileNotFoundException jaxb) {
            log.error("Error parsing file: " + ap, jaxb);
            throw jaxb;
        }

        return newNsa;
    }
}
