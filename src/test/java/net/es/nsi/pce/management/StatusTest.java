package net.es.nsi.pce.management;


import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jaxb.management.StatusType;
import net.es.nsi.pce.jaxb.management.TopologyStatusType;
import net.es.nsi.pce.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hacksaw
 */
public class StatusTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** StatusTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("management").path("status");
        System.out.println("*************************************** StatusTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** StatusTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        System.out.println("*************************************** StatusTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testStatus() {
        // Simple status to determine current state of topology discovery.
        Response response = management.path("topology").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        StatusType status = response.readEntity(StatusType.class);
        System.out.println("Status code = " + status.getStatus());
        assertEquals(TopologyStatusType.COMPLETED, status.getStatus());
        System.out.println("Topology status code = " + status.getStatus().value());
    }
}
