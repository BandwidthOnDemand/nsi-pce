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
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class DescriptionClient {

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);

        final WebTarget webTarget = client.target("http://localhost:8400/descriptions/");

        // Simple ping to determine if interface is available.
        Response response = webTarget.request("application/vnd.net.es.description.v1+json").get();

        System.out.println("Descriptions result " + response.getStatus());
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            System.out.println("Descriptions not available.");
            return;
        }

        response.close();
    }
}
