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
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.StpType;
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
        Response response = webTarget.path("ping").request(MediaType.APPLICATION_JSON).get();
        
        System.out.println("Ping result " + response.getStatus());
       
        // Retrieve a full list of STPs within the topology model.
        CollectionType stps = webTarget.path("stps").request(MediaType.APPLICATION_JSON).get(CollectionType.class);
        
        // Dump the full list of STP.
        System.out.println("Discovered " + stps.getStp().size() + " STPs.");
        for (StpType stp : stps.getStp()) {
            System.out.println(stp.getId());
        }
        
        // Get a list of STP filtered by networkId.
        stps = webTarget.path("stps").queryParam("networkId", "urn:ogf:network:uvalight.net:2013:topology").request(MediaType.APPLICATION_JSON).get(CollectionType.class);

        // Dump the full list of STP.
        System.out.println("Discovered " + stps.getStp().size() + " STPs.");
        for (StpType stp : stps.getStp()) {
            System.out.println(stp.getId());
        }
        
        // Get a list of STP filtered by labelType and value.
        stps = webTarget.path("stps").queryParam("labelType", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan").queryParam("labelValue", "1780").request(MediaType.APPLICATION_JSON).get(CollectionType.class);

        // Dump the full list of STP.
        System.out.println("Discovered " + stps.getStp().size() + " STPs with vlan=1780.");
        for (StpType stp : stps.getStp()) {
            System.out.println(stp.getId());
        }
        
        // Get a specific STP.
        String Target_STP = "stps/urn:ogf:network:icair.org:2013:krlight:vlan=1780";
        response = webTarget.path(Target_STP).queryParam("labelType", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan").queryParam("labelValue", "1780").request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.out.println("readNsaTopology: Failed to retrieve STP " + Target_STP);
            throw new NotFoundException("Failed to retrieve STP " + Target_STP);
        }

        Date lastMod = response.getLastModified();
        System.out.println("Last-Modified = " + lastMod);
        
        StpType stp = response.readEntity(StpType.class);
        System.out.println(stp.getId());
        
        // Now we do the same query but asking for only chnages.
        response = webTarget.path(Target_STP).queryParam("labelType", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan").queryParam("labelValue", "1780").request(MediaType.APPLICATION_XML) .header("If-Modified-Since", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123)).get();
        
        // A 304 Not Modified indicates we already have a up-to-date document.
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            System.out.println("readNsaTopology: NOT_MODIFIED returned " + Target_STP);
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
}
