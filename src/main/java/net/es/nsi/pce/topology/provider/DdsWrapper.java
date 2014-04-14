/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import net.es.nsi.pce.topology.jaxb.DdsDocumentType;

/**
 *
 * @author hacksaw
 */
public class DdsWrapper {
    private long discovered;
    private DdsDocumentType document;

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
    public DdsDocumentType getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(DdsDocumentType document) {
        this.document = document;
    }
}
