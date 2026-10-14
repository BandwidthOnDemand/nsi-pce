/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.es.nsi.pce.jaxb.dds.NotificationListType;
import net.es.nsi.pce.jaxb.dds.NotificationType;
import net.es.nsi.pce.schema.NsiConstants;

/**
 *
 * @author hacksaw
 */
@Path("/dds/")
public class DdsNotificationCallback {

    @POST
    @Path("/callback")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response notification(NotificationListType notify) {
        System.out.println("DdsNotificationCallback: id=" + notify.getId() + ", href=" + notify.getHref() + ", providerId=" + notify.getProviderId());
        for (NotificationType notification : notify.getNotification()) {
            System.out.println("DdsNotificationCallback: event=" + notification.getEvent().value() + ", documentId=" + notification.getDocument().getId());
        }
        TestServer.INSTANCE.pushDiscoveryNotification(notify);
        return Response.accepted().build();
    }
}
