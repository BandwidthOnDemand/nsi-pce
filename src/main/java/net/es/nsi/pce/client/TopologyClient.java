/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.client;

import java.util.Date;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jaxb.management.LogListType;
import net.es.nsi.pce.jaxb.management.LogType;
import net.es.nsi.pce.jaxb.management.StatusType;
import net.es.nsi.pce.jaxb.topology.CollectionType;
import net.es.nsi.pce.jaxb.topology.NetworkType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.jersey.RestClient;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class TopologyClient {

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);

        final WebTarget webTarget = client.target("http://localhost:8400/topology/");

        // Simple ping to determine if interface is available.
        Response response = webTarget.path("ping").request(MediaType.APPLICATION_XML).get();

        System.out.println("Ping result " + response.getStatus());
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.out.println("Topology serivce not available.");
            return;
        }

        // Retrieve topology service status.
        response = webTarget.path("status").request(MediaType.APPLICATION_XML).get();
        System.out.println("Status result " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            StatusType status = response.readEntity(StatusType.class);
            System.out.println("Summary status " + status.getStatus().value());
            System.out.println("Last audit " + status.getLastAudit());
            System.out.println("Last discovered " + status.getLastDiscovered());
        }

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

        // Retrieve a full list of STPs within the topology model.
        response = webTarget.path("stps").request(MediaType.APPLICATION_XML).get();
        System.out.println("Retrieve /topology/stps status = " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            CollectionType stps = response.readEntity(CollectionType.class);
            System.out.println(stps.getStp().size() + " STPs returned.");
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            System.out.println("Resource not found.");
        }
        else {
            System.err.println("Error reading STPs");
        }

        // Get a list of STP filtered by networkId.
        response = webTarget.path("stps").queryParam("networkId", "urn:ogf:network:uvalight.net:2013:topology").request(MediaType.APPLICATION_XML).get();
        System.out.println("Retrieve /topology/stps?networkId=urn:ogf:network:uvalight.net:2013:topology status = " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            CollectionType stps = response.readEntity(CollectionType.class);
            System.out.println(stps.getStp().size() + " STPs returned.");
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            System.out.println("Resource not found.");
        }
        else {
            System.err.println("Error reading STPs");
        }

        // Get a list of STP filtered by labelType and value.
        response = webTarget.path("stps").queryParam("labelType", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan").queryParam("labelValue", "1784").request(MediaType.APPLICATION_XML).get();
        System.out.println("Retrieve /topology/stps with vlan=1784 status = " + response.getStatus());
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            CollectionType stps = response.readEntity(CollectionType.class);
            System.out.println(stps.getStp().size() + " STPs returned.");
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            System.out.println("Resource not found.");
        }
        else {
            System.err.println("Error reading STPs");
        }

        // Get a specific STP.
        String Target_STP = "stps/urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:526?vlan=1784";
        response = webTarget.path(Target_STP).request(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.err.println("Failed to retrieve STP " + Target_STP);
        }
        else {
            Date lastMod = response.getLastModified();
            System.out.println("Last-Modified = " + lastMod);

            StpType stp = response.readEntity(StpType.class);
            System.out.println(stp.getId());

            // Now we do the same query but asking for only changes.
            response = webTarget.path(Target_STP).request(MediaType.APPLICATION_XML) .header("If-Modified-Since", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123)).get();

            // A 304 Not Modified indicates we already have a up-to-date document.
            if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                System.out.println("NOT_MODIFIED returned " + Target_STP);
            }
            else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                System.err.println("Failed to retrieve STP " + Target_STP);
                throw new NotFoundException("Failed to retrieve STP " + Target_STP);
            }
            else {
                stp = response.readEntity(StpType.class);
                System.out.println("Should not get this: " + stp.getId());
            }
        }

        System.out.println("Get list of all Networks.");

        // Retrieve a list of all the networks.
        CollectionType networks = webTarget.path("networks").request(MediaType.APPLICATION_XML).get(CollectionType.class);
        System.out.println("Networks:");
        for (NetworkType network : networks.getNetwork()) {
            System.out.println(network.getId());
        }

        System.out.println("Get a list of networks filtered by NSA Id.");

        networks = webTarget.path("networks").queryParam("nsaId", "urn:ogf:network:uvalight.net:2013:nsa").request(MediaType.APPLICATION_XML).get(CollectionType.class);
        System.out.println("Filtered Networks:");
        for (NetworkType network : networks.getNetwork()) {
            System.out.println(network.getId());
        }

        System.out.println("Get a specific Network.");

        // Get a specific Network.
        String Target_Network = "networks/urn:ogf:network:uvalight.net:2013:Topology";
        response = webTarget.path(Target_Network).request(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.err.println("Failed to retrieve Network " + Target_Network);
        }
        else {
            System.out.println("Get a list of all STP in a specific Network.");

            // Get STPs under a specific Network.
            String Target_Network_STPS = "networks/urn:ogf:network:uvalight.net:2013:Topology/stps";
            response = webTarget.path(Target_Network_STPS).request(MediaType.APPLICATION_XML).get();

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                System.out.println("Failed to retrieve Network " + Target_Network_STPS);
                throw new NotFoundException("Failed to retrieve Network " + Target_Network_STPS);
            }
        }

        System.out.println("Get a list of all available NSA.");

        // Get a list of available NSA.
        String Target_NSA = "nsas";
        response = webTarget.path(Target_NSA).request(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.err.println("Failed to retrieve NSAs " + Target_NSA);
        }

        System.out.println("Read specific NSA entry.");

        // Get specific NSA entry.
        String Target_NSA_Id = "nsas/urn:ogf:network:ampath.net:2013:nsa";
        response = webTarget.path(Target_NSA_Id).request(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.out.println("Failed to retrieve NSA " + Target_NSA_Id);
        }
        else {
            // Get a list of available ServiceDomains.
            String Target_ServiceDomains = "serviceDomains";
            response = webTarget.path(Target_ServiceDomains).request(MediaType.APPLICATION_XML).get();

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                System.out.println("Failed to retrieve ServiceDomains " + Target_ServiceDomains);
                throw new NotFoundException("Failed to retrieve ServiceDomains " + Target_ServiceDomains);
            }
        }
    }
}
