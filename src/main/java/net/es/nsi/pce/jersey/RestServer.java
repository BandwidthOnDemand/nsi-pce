/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.jersey;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.api.FindPathService;
import net.es.nsi.pce.api.TopologyService;
import net.es.nsi.pce.api.jaxb.FindPathErrorType;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
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
                .register(TopologyService.class) // Remove this if packages gets fixed.
                .register(new MoxyXmlFeature())
                .register(new MoxyJsonFeature())
                .registerInstances(new JsonMoxyConfigurationContextResolver());
    }
    
    public static Response getBadRequestError(String element) {
        ObjectFactory factory = new ObjectFactory();
        
        FindPathErrorType error = factory.createFindPathErrorType();
        error.setMessage("Invalid " + element + " element");
        return Response.status(Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build(); 
    }
}
