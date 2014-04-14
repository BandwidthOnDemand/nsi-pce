/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.dao;

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
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.provider.DiscoveryParser;
import net.es.nsi.pce.discovery.provider.Document;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
public class DocumentCache {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // The holder of our configuration.
    private DiscoveryConfiguration configReader;
    
    // In-memory document cache indexed by nsa/type/id.
    private Map<String, Document> documents = new ConcurrentHashMap<>();
    
    private boolean useCache = false;
    private String cachePath;
    
    private boolean useDocuments = false;
    private String documentPath;
    
    public DocumentCache(DiscoveryConfiguration configReader) throws FileNotFoundException {
        this.configReader = configReader;
        
        // Load all documents within the cache directory.
        if (configReader.getCache() != null && !configReader.getCache().isEmpty()) {
            File dir = new File(configReader.getCache());
            cachePath = dir.getAbsolutePath();
            log.debug("load: cache directory configured " + cachePath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new FileNotFoundException("Cannot create directory: " + cachePath);                
                }  
            }

            // We will be using the cache for this deployment.
            useCache = true;
        }
        
        // We also load documents from the local cache at startup.
        if (configReader.getDocuments() != null && !configReader.getDocuments().isEmpty()) {
            File dir = new File(configReader.getDocuments());
            documentPath = dir.getAbsolutePath();
            log.debug("load: local document directory configured " + documentPath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new FileNotFoundException("Cannot create directory: " + documentPath);                
                }  
            }
        
            useDocuments = true;
        }
    }
    
    public static DocumentCache getInstance() {
        DocumentCache documentCache = SpringApplicationContext.getBean("documentCache", DocumentCache.class);
        return documentCache;
    }
    
    public Document get(String id) {
        return documents.get(id);
    }
    
    public Document put(String id, Document doc) throws JAXBException, IOException {
        // Store the document.
        Document result = documents.put(id, doc);
        
        // We need to write this new document to disk.
        if (useCache) {
            String filename = Paths.get(cachePath, UUID.randomUUID().toString() + ".xml").toString();
            doc.setFilename(filename);
            log.debug("put: adding new file " + filename);

            DiscoveryParser.getInstance().writeDocument(filename, doc.getDocument());
        }
        return result;
    }
    
    public Document update(String id, Document doc) throws JAXBException, IOException {   
        // Store the document.
        Document result = documents.put(id, doc);
        
        // We need to write this new document to disk.
        if (useCache) {
            String filename = doc.getFilename();
            if (result != null) {
                filename = result.getFilename();
                doc.setFilename(filename);
                log.debug("update: reusing old filename " + filename);
            }
            
            if (filename == null || filename.isEmpty()) {
                filename = Paths.get(cachePath, UUID.randomUUID().toString() + ".xml").toString();
                doc.setFilename(filename);
            }
            log.debug("update: updating " + filename);
            DiscoveryParser.getInstance().writeDocument(filename, doc.getDocument());
        }
        
        return result;
    }
    
    public Document remove(String id) {
        Document doc = documents.remove(id);
        if (doc != null && isUseCache()) {
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
        loadCache();
        loadDocuments();
    }

    private void loadCache() {
        log.debug("loadCache: entering");
        
        if (!useCache) {
            log.debug("loadCache: cache directory not configured so exiting.");
            return;
        }
        
        loadDirectory(cachePath, true);
    }
    
    private void loadDocuments() {
        log.debug("loadDocuments: entering");
        
        if (!useDocuments) {
            log.debug("loadDocuments: local document directory not configured so exiting.");
            return;
        }
        
        loadDirectory(documentPath, false);
    }
    
    
    private void loadDirectory(String path, boolean delete) {
        Collection<String> xmlFilenames = XmlUtilities.getXmlFilenames(path);

        for (String filename : xmlFilenames) {
            log.debug("loadDirectory: filename " + filename);
            DocumentType document;
            try {
                document = DiscoveryParser.getInstance().readDocument(filename);
                if (document == null) {
                    log.error("loadDirectory: Loaded empty document from " + filename);
                    deleteFile(filename, delete);
                    continue;
                }
            }
            catch (JAXBException | IOException ex) {
                log.error("loadDirectory: Failed to load file " + filename, ex);
                deleteFile(filename, delete);
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
                    log.error("loadDirectory: Loaded document has expired " + filename + ", expires=" + expires.toGregorianCalendar().getTime().toString());
                    deleteFile(filename, delete);
                    continue;
                }
            }
            else {
                // No expire value provided so make one.
                Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
                XMLGregorianCalendar xmlGregorianCalendar;
                try {
                    xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
                } catch (DatatypeConfigurationException ex) {
                    log.error("loadDirectory: Document does not contain an expires date and creation of one failed id=" + document.getId());
                    continue;
                }

                document.setExpires(xmlGregorianCalendar);
            }

            Document entry = new Document(document);
            entry.setFilename(filename);

            // Make sure the file we are loading does not overwrite a newer
            // version of the document.  If it is an older version then remove
            // it from the cache and disk.
            Document result = documents.get(entry.getId());

            if (result == null) {
                documents.put(entry.getId(), entry);
                log.debug("loadDirectory: added document id=" + entry.getId() + ", filename=" + filename);                
            }
            else if (entry.getDocument().getVersion().compare(result.getDocument().getVersion()) == DatatypeConstants.GREATER) {
                log.debug("loadDirectory: new document found so removing old cached document id=" + result.getId() + ", filename="+ result.getFilename());
                documents.remove(result.getId());
                deleteFile(result.getFilename(), delete);
                documents.put(entry.getId(), entry);
                log.debug("loadDirectory: added new document id=" + entry.getId() + ", filename=" + filename);
            }
            else {
                log.debug("loadDirectory: document currently in cache is newer, removing old document id=" + entry.getId() + ", filename=" + filename);
                deleteFile(filename, delete);
            }
        }
        
        log.debug("loadDirectory: exiting");
    }
    
    public void expire() {
        for (Document document : documents.values()) {
            // We need to determine if this document is still valid
            // before proceding.
            DocumentType doc = document.getDocument();
            XMLGregorianCalendar expires = doc.getExpires();
            if (expires != null) {
                Date expiresTime = expires.toGregorianCalendar().getTime();

                // We take the current time and add the expiry buffer.
                Date now = new Date();
                now.setTime(now.getTime() + configReader.getExpiryInterval() * 1000);
                if (expiresTime.before(now)) {
                    // This document is old and no longer valid.
                    log.debug("expire: document has expired " + document.getId() + ", expires=" + expires.toGregorianCalendar().getTime().toString());
                    this.remove(document.getId());
                }
            }
        }
    }

    /**
     * @return the useCache
     */
    public boolean isUseCache() {
        return useCache;
    }

    /**
     * @return the useDocuments
     */
    public boolean isUseDocuments() {
        return useDocuments;
    }
    
    public void deleteFile(String filename, boolean delete) {
        if (delete) {
            File file = new File(filename);
            if(!file.delete()) {
                log.error("DocumentCache: Delete failed for file  " + filename);
            }
            else {
                log.debug("DocumentCache: Deleted old cache document " + filename);
            }
        }
    }
}
