/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.provider;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import net.es.nsi.pce.discovery.api.DiscoveryError;
import net.es.nsi.pce.discovery.jaxb.DocumentType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class Document implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String DOCUMENTS_URL = "/discovery/documents/";
    private static ObjectFactory factory = new ObjectFactory();
    
    private String id;
    private DocumentType document;
    private Date lastDiscovered = new Date();
    
    public Document(DocumentType document) throws IllegalArgumentException {
        this.id = documentId(document.getNsa(), document.getType(), document.getId());
        this.document = document;
        this.document.setNsa(this.document.getNsa().trim());
        this.document.setType(this.document.getType().trim());
        this.document.setId(this.document.getId().trim());
        this.document.setHref(DOCUMENTS_URL + this.id);
    }

    public static String documentId(String nsa, String type, String id) throws IllegalArgumentException {
        if (nsa == null || nsa.trim().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
            throw new IllegalArgumentException(error);
        }
        else if (type == null || type.trim().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "type");
            throw new IllegalArgumentException(error);
        }
        else if (id == null || id.trim().isEmpty()) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "document", "id");
            throw new IllegalArgumentException(error);
        }
        
        StringBuilder sb = new StringBuilder();
        
        try {
            sb.append(URLEncoder.encode(nsa.trim(), "UTF-8"));
            sb.append("/").append(URLEncoder.encode(type.trim(), "UTF-8"));
            sb.append("/").append(URLEncoder.encode(id.trim(), "UTF-8"));
        }
        catch (UnsupportedEncodingException ex) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_INVALID, "document", "id");
            throw new IllegalArgumentException(error);            
        }
        return sb.toString();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the document
     */
    public DocumentType getDocument() {
        return document;
    }
    
    /**
     * @return the document
     */
    public DocumentType getDocumentSummary() {
        DocumentType newDocType = factory.createDocumentType();
        newDocType.setExpires(document.getExpires());
        newDocType.setHref(document.getHref());
        newDocType.setId(document.getId());
        newDocType.setNsa(document.getNsa());
        newDocType.setType(document.getType());
        newDocType.setVersion(document.getVersion());
        return newDocType;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(DocumentType document) {
        this.document = document;
    }

    /**
     * @return the lastModified
     */
    public Date getLastDiscovered() {
        return lastDiscovered;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastDiscovered(Date lastModified) {
        this.lastDiscovered = lastModified;
    }
}
