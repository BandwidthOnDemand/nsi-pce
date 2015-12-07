package net.es.nsi.pce.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jaxb.management.LogListType;
import net.es.nsi.pce.jaxb.management.LogType;
import net.es.nsi.pce.jaxb.management.StatusType;
import net.es.nsi.pce.jaxb.management.TimerListType;
import net.es.nsi.pce.jaxb.management.TimerType;
import net.es.nsi.pce.jersey.RestClient;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class ManagementClient {

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);

        final WebTarget webTarget = client.target("http://localhost:8400/management/");

        // Retrieve topology service status.
        Response response = webTarget.path("status/topology").request(MediaType.APPLICATION_XML).get();
        System.out.println("Status result " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            StatusType status = response.readEntity(StatusType.class);
            System.out.println("Summary status " + status.getStatus().value());
            System.out.println("Last audit " + status.getLastAudit());
            System.out.println("Last discovered " + status.getLastDiscovered());
        }

        response.close();

        // Retrieve topology service status.
        response = webTarget.path("logs").request(MediaType.APPLICATION_XML).get();
        System.out.println("Logs result " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            LogListType logs = response.readEntity(LogListType.class);
            for (LogType log : logs.getLog()) {
                System.out.println("Retreiving: " + log.getId());

                response = webTarget.path("logs/" + log.getId()).request(MediaType.APPLICATION_XML).get();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    System.out.println("Got it: " + log.getId());
                }
                else {
                    System.out.println("Error reading: " + log.getId() + " (" + response.getStatus() + ")");
                }
            }
        }

        response.close();

        // Retrieve NSI-PCE timers.
        response = webTarget.path("timers").request(MediaType.APPLICATION_XML).get();
        System.out.println("Timers result " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            TimerListType timers = response.readEntity(TimerListType.class);
            for (TimerType timer : timers.getTimer()) {
                System.out.println("Retreiving timer: " + timer.getId());

                response = webTarget.path("timers/" + timer.getId()).request(MediaType.APPLICATION_XML).get();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    System.out.println("Got it: " + timer.getId());
                }
                else {
                    System.out.println("Error reading: " + timer.getId() + " (" + response.getStatus() + ")");
                }
            }
        }

        response.close();
        client.close();
    }
}