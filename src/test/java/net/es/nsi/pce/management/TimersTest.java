package net.es.nsi.pce.management;


import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.jersey.RestServer;
import net.es.nsi.pce.management.jaxb.ObjectFactory;
import net.es.nsi.pce.management.jaxb.TimerListType;
import net.es.nsi.pce.management.jaxb.TimerStatusType;
import net.es.nsi.pce.management.jaxb.TimerType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hacksaw
 */
public class TimersTest extends JerseyTest {
    final WebTarget root = target();
    final WebTarget topology = target().path("management");
    
    @Override
    protected Application configure() {
        // Configure test instance of PCE server.
        try {
            ConfigurationManager.INSTANCE.initialize("src/test/resources/config-SwitchingService/");
        } catch (Exception ex) {
            System.err.println("configure(): Could not initialize test environment." + ex.toString());
            fail("configure(): Could not initialize test environment.");
        }
        Application app = new Application();
        app.getProperties();
        return RestServer.getConfig(ConfigurationManager.INSTANCE.getPceConfig().getPackageName());
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        RestClient.configureClient(clientConfig);
    }
    
    @Test
    public void getAllTimers() {
        Response response = topology.path("timers").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        TimerListType timerList = response.readEntity(TimerListType.class);
        
        assertNotNull(timerList);

        for (TimerType timer : timerList.getTimer()) {
            response = topology.path("timers/" + timer.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            
            TimerType readTimer = response.readEntity(TimerType.class);
            System.out.println("Read timer: " + readTimer.getId());
        }
    }
    
    @Test
    public void modifyTimer() {
        Response response = topology.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        TimerType timer = response.readEntity(TimerType.class);
        
        assertNotNull(timer);

        timer.setTimerInterval(30000);
        
        ObjectFactory managementFactory = new ObjectFactory();
        JAXBElement<TimerType> createTimer = managementFactory.createTimer(timer);
        
        response = topology.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerType>>(createTimer) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = topology.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        timer = response.readEntity(TimerType.class);
        assertEquals(timer.getTimerInterval(), 30000);
    }
    
    @Test
    public void haultandScheduleTimer() {
        Response response = topology.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        TimerType timer = response.readEntity(TimerType.class);
        
        assertNotNull(timer);
        
        // The timer should be idle based on default configuration.
        assertEquals(TimerStatusType.SCHEDULED, timer.getTimerStatus());

        // We need to hault the timer.
        ObjectFactory managementFactory = new ObjectFactory();
        JAXBElement<TimerStatusType> statusElement = managementFactory.createTimerStatus(TimerStatusType.HAULTED);
        
        response = topology.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerStatusType>>(statusElement) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        TimerStatusType status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.HAULTED, status);

        response = topology.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        timer = response.readEntity(TimerType.class);
        assertEquals(TimerStatusType.HAULTED, timer.getTimerStatus());
        
        // Force a run of the job when not scheduled.
        statusElement = managementFactory.createTimerStatus(TimerStatusType.RUNNING);
        
        response = topology.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerStatusType>>(statusElement) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.RUNNING, status);
        
        while(true) {
            System.out.println("User forced audit...");
            try { Thread.sleep(2000); } catch (Exception ex) {}
            
            response = topology.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
            status = response.readEntity(TimerStatusType.class);
            if (status != TimerStatusType.RUNNING) {
                System.out.println("User forced audit done...");
                break;
            }
        }
        
        // Force a run of the job when not scheduled.
        statusElement = managementFactory.createTimerStatus(TimerStatusType.SCHEDULED);
        
        response = topology.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerStatusType>>(statusElement) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.SCHEDULED, status);
        
        response = topology.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.SCHEDULED, status);
    }
}
