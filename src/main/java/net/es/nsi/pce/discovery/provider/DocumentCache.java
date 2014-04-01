/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.schema.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class DocumentCache {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // The holder of our configuration.
    private ConfigurationReader configReader;
    
    // In-memory document cache indexed by nsa/type/id.
    private Map<String, Document> documents = new ConcurrentHashMap<>();
    
    public DocumentCache(ConfigurationReader configReader) {
        this.configReader = configReader;
    }
    
    public Document get(String id) {
        return documents.get(id);
    }
    
    public Document put(String id, Document doc) throws JAXBException, IOException {
        // Store the document.
        Document result = documents.put(id, doc);
        
        // We need to write this new document to disk.
        String filename = Paths.get(System.getProperty("user.dir"), configReader.getCache(), UUID.randomUUID().toString() + ".xml").toString();
        doc.setFilename(filename);
        log.debug("put: adding new file " + filename);
        
        DiscoveryParser.getInstance().writeDocument(filename, doc.getDocument());
        
        return result;
    }
    
    public Document update(String id, Document doc) throws JAXBException, IOException {
        // Store the document.
        Document result = documents.put(id, doc);
        
        // We need to write this new document to disk.
        log.debug("update: updating " + doc.getFilename());
        DiscoveryParser.getInstance().writeDocument(doc.getFilename(), doc.getDocument());
        
        return result;
    }
    
    public Document remove(String id) throws JAXBException, IOException {
        Document doc = documents.remove(id);
        if (doc != null) {
            log.debug("remove: removing " + doc.getFilename());
            File file = new File(doc.getFilename());
            if(!file.delete()) {
                log.error("remove: Delete failed for file  " + doc.getFilename());
            }
        }

        return doc;
    }
    
    public Collection<Document> values() {
        return documents.values();
    }

    public void load() {
        log.debug("load: entering");
        Collection<String> xmlFilenames = XmlUtilities.getXmlFilenames(configReader.getCache());

        for (String filename : xmlFilenames) {
            log.debug("load: filename " + filename);
            DocumentType document;
            try {
                document = DiscoveryParser.getInstance().readDocument(filename);
                if (document == null) {
                    log.error("load: Loaded empty document from " + filename);
                    continue;
                }
            }
            catch (JAXBException | FileNotFoundException ex) {
                log.error("load: Failed to load file " + filename, ex);
                continue;
            }

            // We need to determine if this document is still valid
            // before proceding.
            XMLGregorianCalendar expires = document.getExpires();
            if (expires != null) {
                Date expiresTime = expires.toGregorianCalendar().getTime();

                // We take the current time and add the expiry buffer.
                Date now = new Date();
                now.setTime(now.getTime() + configReader.getExpiryInterval() * 1000);
                if (expiresTime.before(now)) {
                    // This document is old and no longer valid.
                    log.error("load: Loaded document has expired " + filename + ", expires=" + expires.toGregorianCalendar().getTime().toString());
                    File file = new File(filename);
                    if(!file.delete()) {
                        log.error("load: Delete failed for file  " + filename);
                    }
                    continue;
                }
            }

            Document entry = new Document(document);
            entry.setFilename(filename);

            // Make sure the file we are loading does not overwrite a newer
            // version of the document.  If it is an older version then remove
            // it from the cache and disk.
            Document result = documents.get(entry.getId());

            if (result == null) {
                documents.put(entry.getId(), entry);
                log.debug("load: added document id=" + entry.getId() + ", filename="+ filename);                
            }
            else if (entry.getDocument().getVersion().compare(result.getDocument().getVersion()) == DatatypeConstants.GREATER) {
                log.debug("load: removing cached document id=" + result.getId() + ", filename="+ result.getFilename());
                documents.remove(result.getId());
                documents.put(entry.getId(), entry);
                log.debug("load: added document id=" + entry.getId() + ", filename="+ filename);
            }
            else {
                log.debug("load: removing cached document id=" + entry.getId() + ", filename="+ filename);
                File file = new File(filename);
                if(!file.delete()) {
                    log.error("load: Delete failed for file  " + filename);
                }                
            }
        }
        
        log.debug("load: exiting");
    }
}
