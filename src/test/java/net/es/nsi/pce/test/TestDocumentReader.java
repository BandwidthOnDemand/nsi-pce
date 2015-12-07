package net.es.nsi.pce.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.dds.CollectionType;
import net.es.nsi.pce.jaxb.dds.DocumentListType;
import net.es.nsi.pce.jaxb.dds.DocumentType;
import net.es.nsi.pce.schema.DdsParser;
import net.es.nsi.pce.topology.provider.DdsWrapper;
import net.es.nsi.pce.topology.provider.DocumentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class replaces the DDS Document Reader for test cases.  It will read
 * a file containing the list of DDS documents held within a <collection>
 * element.
 */
public class TestDocumentReader implements DocumentReader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // The remote location of the file to read.
    private String target;

    // The type of document to read.
    private String type;

    // Time we last read the master topology.
    private long lastModified = 0;

    // A list of full documents matching the specified type.
    private final Map<String, DdsWrapper> ddsDocuments = new ConcurrentHashMap<>();

    // Documents of the specified type discovered as local to this DDS service.
    private final DocumentListType localDocuments = new DocumentListType();

    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     *
     * @param target Location of the NSA's XML based NML topology.
     * @param type
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

    private CollectionType collection = null;
    private boolean read() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        if (collection == null) {
            try {
                collection = DdsParser.getInstance().readCollection(target);
            } catch (IOException ex) {
                log.error("File not found " + target, ex);
                throw new NotFoundException("File does not exist " + target);
            }
        }
        else {
            return false;
        }

        // Read and store local documents.
        for (DocumentType document : collection.getLocal().getDocument()) {
            if (type.equals(document.getType().trim())) {
                localDocuments.getDocument().add(document);
            }
        }

        long currentTime = System.currentTimeMillis();
        for (DocumentType document : collection.getDocuments().getDocument()) {
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
    public DocumentListType getLocalDocuments() {
        return localDocuments;
    }
}
