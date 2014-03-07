/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.schema;

import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.path.jaxb.FindPathErrorType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.pf.api.NsiError;

/**
 *
 * @author hacksaw
 */
public class PathApiTest {
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ObjectFactory factory = new net.es.nsi.pce.path.jaxb.ObjectFactory();
        FindPathErrorType error;
        error = NsiError.getFindPathError(NsiError.CAPACITY_UNAVAILABLE, "urn:ogf:network:icair.org:2013:stp:port1234?vlan=2222", "2000000");
        System.out.println(error.getCode());
        JAXBElement<FindPathErrorType> errorElement = factory.createFindPathError(error);
        System.out.println(errorElement.getValue().getCode());
        String xml = PathApiParser.getInstance().jaxbToString(errorElement);
        System.out.println(xml);

        errorElement = (JAXBElement<FindPathErrorType>) PathApiParser.getInstance().stringToJaxb(xml);
        System.out.println(errorElement.getValue().getCode());
    }
}
