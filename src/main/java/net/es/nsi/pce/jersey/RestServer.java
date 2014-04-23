/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.jersey;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.discovery.api.DiscoveryService;
import net.es.nsi.pce.path.api.FindPathService;
import net.es.nsi.pce.management.api.ManagementService;
import net.es.nsi.pce.path.api.ReachabilityService;
import net.es.nsi.pce.topology.api.TopologyService;
import net.es.nsi.pce.path.jaxb.FindPathErrorType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.pf.api.NsiError;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author hacksaw
 */
public class RestServer {
    public static ResourceConfig getConfig(String packageName) {
        return new ResourceConfig()
                .packages(packageName) // This seems to be broken when run outside of Jersey test.
                .register(FindPathService.class) // Remove this if packages gets fixed.
                .register(ReachabilityService.class) // Remove this if packages gets fixed.
                .register(TopologyService.class) // Remove this if packages gets fixed.
                .register(DiscoveryService.class) // Remove this if packages gets fixed.
                .register(ManagementService.class) // Remove this if packages gets fixed.
                .register(new MoxyXmlFeature())
                .register(new MoxyJsonFeature())
                .registerInstances(new JsonMoxyConfigurationContextResolver());
    }

    public static Response getBadRequestError(String resource, String details) {
        ObjectFactory factory = new ObjectFactory();
        FindPathErrorType error = NsiError.getFindPathError(NsiError.MISSING_PARAMETER, resource, details);
        return Response.status(Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
    }

    public static Response getInternalServerError(String resource, String details) {
        ObjectFactory factory = new ObjectFactory();
        FindPathErrorType error = NsiError.getFindPathError(NsiError.INTERNAL_ERROR, resource, details);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
    }
}
