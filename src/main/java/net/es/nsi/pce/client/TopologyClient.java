/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.StpType;
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
        StpType stp = webTarget.path("stps/urn:ogf:network:icair.org:2013:krlight:vlan=1780").queryParam("labelType", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan").queryParam("labelValue", "1780").request(MediaType.APPLICATION_JSON).get(StpType.class);
        
        System.out.println(stp.getId());
    }
}
