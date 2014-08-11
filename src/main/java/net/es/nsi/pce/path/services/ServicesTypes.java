/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path.services;

/**
 *
 * @author hacksaw
 */
public class ServicesTypes {
    public static final String NAMESPACE_SERVICES_TYPES = "http://schemas.ogf.org/nsi/2013/12/services/types";
    public static final String STPID = "http://schemas.ogf.org/nsi/2013/12/services/types#stpId";
    private static final Namespace stpId = new Namespace(NAMESPACE_SERVICES_TYPES, STPID, "stpId");

    /**
     * @return the stpId
     */
    public static Namespace getStpId() {
        return stpId;
    }
}
