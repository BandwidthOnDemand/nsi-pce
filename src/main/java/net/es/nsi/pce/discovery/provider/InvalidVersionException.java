/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.provider;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author hacksaw
 */
public class InvalidVersionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final XMLGregorianCalendar version;
    private final XMLGregorianCalendar actual;
    
    public InvalidVersionException(final String msg, final XMLGregorianCalendar version, final XMLGregorianCalendar actual) {
        super( msg );
        this.version = version;
        this.actual = actual;
    }
    
    public XMLGregorianCalendar getVersion() {
        return version;
    }

    /**
     * @return the actual
     */
    public XMLGregorianCalendar getActual() {
        return actual;
    }
}
