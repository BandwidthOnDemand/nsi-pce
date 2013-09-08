package net.es.nsi.pce.services;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * A simple test to verify correct operation of the Service mappings.
 * 
 * @author hacksaw
 */
public class ServiceTest {
    @Test
    public void test() {
        // Get service by serviceType string - SUCCESS.
        List<Service> serviceList = Service.getServiceByType("http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE");
        
        for (Service service : serviceList) {
            assertEquals(Service.EVTS, service);
        }
        
        // Get service by serviceType string - FAILED.
        serviceList = Service.getServiceByType("http://services.ogf.org/nsi/2013/07/descriptions/EVTS.POOP");
        assertNull(serviceList);
        
        // Get service by QNAME string - SUCCESS.
        Service service = Service.getService("{http://schemas.ogf.org/nsi/2013/07/services/point2point}p2ps");
        assertEquals("{http://schemas.ogf.org/nsi/2013/07/services/point2point}p2ps", service.toString());
        
        // Get service by QNAME string - SUCCESS.
        service = Service.getService("{http://schemas.ogf.org/nsi/2013/07/services/point2point}evts");
        assertEquals("{http://schemas.ogf.org/nsi/2013/07/services/point2point}evts", service.toString());
        
        // Get service by QNAME string - SUCCESS.
        service = Service.getService("{http://schemas.ogf.org/nsi/2013/07/services/point2point}ets");
        assertEquals("{http://schemas.ogf.org/nsi/2013/07/services/point2point}ets", service.toString());
        
        // Get service by QNAME string - FAILED.
        service = Service.getService("{http://schemas.ogf.org/nsi/2013/07/services/point2point}poop");
        assertEquals(Service.UNSUPPORTED, service);
    }
}
