/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import net.es.nsi.pce.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class WadlTest {
    private static TestConfig testConfig;
    private static WebTarget target;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** WadlTest oneTimeSetUp ***********************************");
        // Configure the local test client callback server.
        testConfig = new TestConfig();
        target = testConfig.getTarget();
        System.out.println("*************************************** WadlTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** WadlTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        System.out.println("*************************************** WadlTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testApplicationWadl() {
        final WebTarget webTarget = target.path("application.wadl");
        String serviceWadl = webTarget.request(MediaType.APPLICATION_XML).get(String.class);
        assertTrue(serviceWadl.length() > 0);
    }
}
