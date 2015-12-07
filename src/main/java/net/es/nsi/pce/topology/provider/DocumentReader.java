/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.dds.DocumentListType;

/**
 *
 * @author hacksaw
 */
public interface DocumentReader {

    Map<String, DdsWrapper> get() throws NotFoundException, JAXBException, UnsupportedEncodingException;

    Map<String, DdsWrapper> getIfModified() throws NotFoundException, JAXBException, UnsupportedEncodingException;

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * topology document was modified.
     *
     * @return the lastModified date of the remote topology document.
     */
    long getLastModified();

    /**
     * @return the localDocuments
     */
    DocumentListType getLocalDocuments();

    /**
     * Set the last modified date of the cached remote topology document.
     *
     * @param lastModified the lastModified to set
     */
    void setLastModified(long lastModified);

    void setTarget(String target);

    void setType(String type);

}
