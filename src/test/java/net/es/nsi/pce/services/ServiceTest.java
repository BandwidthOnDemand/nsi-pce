package net.es.nsi.pce.services;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * A simple test to verify correct operation of the Service mappings.
 * 
 * @author hacksaw
 */
public class ServiceTest {
    private final static String SERVICETYPE = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
    
    @Test
    public void test() {
        // Get service by serviceType string - SUCCESS.
        List<Service> serviceList = Service.getServiceByType(SERVICETYPE);
        
        assertNotNull(serviceList);
        
        for (Service service : serviceList) {
            assertEquals(Service.P2PS, service);
        }
        
        // Get service by serviceType string - FAILED.
        serviceList = Service.getServiceByType(SERVICETYPE + ".crud");
        assertEquals(0, serviceList.size());
        
        // Get service by QNAME string - SUCCESS.
        Service service = Service.getService("{http://schemas.ogf.org/nsi/2013/12/services/point2point}p2ps");
        assertEquals("{http://schemas.ogf.org/nsi/2013/12/services/point2point}p2ps", service.toString());
        
        // Get service by QNAME string - FAILED.
        service = Service.getService("{http://schemas.ogf.org/nsi/2013/12/services/point2point}crud");
        assertEquals(Service.UNSUPPORTED, service);
    }
}
