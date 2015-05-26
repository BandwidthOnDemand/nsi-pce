/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path.services;

/**
 *
 * @author hacksaw
 */
public class Namespace {
    private final String namespace;
    private final String qname;
    private final String type;

    public Namespace(String namespace, String qname, String type) {
        this.namespace = namespace;
        this.qname = qname;
        this.type = type;
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @return the qname
     */
    public String getQname() {
        return qname;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
}
