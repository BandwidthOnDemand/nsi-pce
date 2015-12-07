/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import net.es.nsi.pce.jaxb.dds.DocumentType;

/**
 *
 * @author hacksaw
 */
public class DdsWrapper {
    private long discovered;
    private DocumentType document;

    /**
     * @return the discovered
     */
    public long getDiscovered() {
        return discovered;
    }

    /**
     * @param discovered the discovered to set
     */
    public void setDiscovered(long discovered) {
        this.discovered = discovered;
    }

    /**
     * @return the document
     */
    public DocumentType getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(DocumentType document) {
        this.document = document;
    }
}
