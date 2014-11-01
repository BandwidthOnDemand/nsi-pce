package net.es.nsi.pce.test;

import net.es.nsi.pce.topology.provider.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.topology.jaxb.DdsDocumentListType;
import net.es.nsi.pce.schema.NmlParser;
import net.es.nsi.pce.topology.jaxb.DdsDocumentType;
import net.es.nsi.pce.topology.jaxb.DdsCollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDocumentReader implements DocumentReader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // The remote location of the file to read.
    private String target;

    // The type of document to read.
    private String type;

    // Time we last read the master topology.
    private long lastModified = 0;

    // A list of full documents matching the specified type.
    private Map<String, DdsWrapper> ddsDocuments = new ConcurrentHashMap<>();

    // Documents of the specified type discovered as local to this DDS service.
    private DdsDocumentListType localDocuments = new DdsDocumentListType();

    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     *
     * @param target Location of the NSA's XML based NML topology.
     */
    public TestDocumentReader(String target, String type) {
        this.target = target;
        this.type = type;
    }

    public TestDocumentReader() {
    }

    @Override
    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * topology document was modified.
     *
     * @return the lastModified date of the remote topology document.
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Set the last modified date of the cached remote topology document.
     *
     * @param lastModified the lastModified to set
     */
    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    private DdsCollectionType collection = null;
    private boolean read() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        if (collection == null) {
            try {
                collection = NmlParser.getInstance().parseDdsCollectionFromFile(target);
            } catch (IOException ex) {
                log.error("File not found " + target, ex);
                throw new NotFoundException("File does not exist " + target);
            }
        }
        else {
            return false;
        }

        // Read and store local documents.
        for (DdsDocumentType document : collection.getLocal().getDocument()) {
            if (type.equals(document.getType().trim())) {
                localDocuments.getDocument().add(document);
            }
        }
        
        long currentTime = System.currentTimeMillis();
        for (DdsDocumentType document : collection.getDocuments().getDocument()) {
            if (type.equals(document.getType().trim())) {
                DdsWrapper wrapper = new DdsWrapper();
                wrapper.setDocument(document);
                wrapper.setDiscovered(currentTime - currentTime % 1000);
                ddsDocuments.put(document.getId(), wrapper);
                log.debug("type=" + type + ", id=" + document.getId());
            }
        }

        return true;
    }

    @Override
    public Map<String, DdsWrapper> get() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        read();
        return Collections.unmodifiableMap(ddsDocuments);
    }

    @Override
    public Map<String, DdsWrapper> getIfModified() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        if (read() == true) {
            return Collections.unmodifiableMap(ddsDocuments);
        }

        return null;
    }

    /**
     * @return the localDocuments
     */
    @Override
    public DdsDocumentListType getLocalDocuments() {
        return localDocuments;
    }
}
