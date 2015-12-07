/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.schema;

import com.google.common.base.Strings;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.jaxb.path.FindPathErrorType;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.NsiError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class PathApiTest {

    @Test
    public void testSuccess() throws Exception {
        ObjectFactory factory = new net.es.nsi.pce.jaxb.path.ObjectFactory();
        FindPathErrorType error;
        error = NsiError.getFindPathError(NsiError.CAPACITY_UNAVAILABLE, Point2PointTypes.getCapacity().getNamespace(), Point2PointTypes.getCapacity().getType(), "2000000");
        assertNotNull(error);

        assertEquals(error.getCode(), NsiError.CAPACITY_UNAVAILABLE.getCode());

        JAXBElement<FindPathErrorType> errorElement = factory.createFindPathError(error);
        assertEquals(errorElement.getValue().getCode(), NsiError.CAPACITY_UNAVAILABLE.getCode());

        String xml = PathApiParser.getInstance().jaxbToString(errorElement);
        assertFalse(Strings.isNullOrEmpty(xml));

        errorElement = (JAXBElement<FindPathErrorType>) PathApiParser.getInstance().stringToJaxb(xml);
        assertEquals(errorElement.getValue().getCode(), NsiError.CAPACITY_UNAVAILABLE.getCode());
    }
}
