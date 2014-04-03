/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.pce.discovery.jaxb.NotificationListType;
import net.es.nsi.pce.schema.MediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/discovery/")
public class DiscoveryNotificationCallback {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @POST
    @Path("/callback")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaTypes.NSI_DDS_V1_JSON, MediaTypes.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaTypes.NSI_DDS_V1_JSON, MediaTypes.NSI_DDS_V1_XML })
    public Response notification(NotificationListType notify) {
        System.out.println("notification: " + notify.getId() + ", " + notify.getHref() + ", " + notify.getProviderId());
        TestServer.INSTANCE.pushDiscoveryNotification(notify);
        return Response.accepted().build();
    }
}
