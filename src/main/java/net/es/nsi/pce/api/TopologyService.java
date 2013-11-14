/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.api;

import java.util.Iterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/topology")
public class TopologyService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response ping() throws Exception {
        return Response.ok().build();
    }
    
    @GET
    @Path("/stps")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public JAXBElement<CollectionType> getStps(
            @QueryParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @QueryParam("labelType") String labelType,
            @QueryParam("labelValue") String labelValue) throws Exception {
        
        // Get a reference to topology provider and get the NSI Topology model.
        TopologyProvider topologyProvider = ConfigurationManager.getTopologyProvider();
        NsiTopology nsiTopology = topologyProvider.getTopology();
        
        // We are stuffing the results into a collection object.
        ObjectFactory nsiFactory = new ObjectFactory();
        CollectionType stps = nsiFactory.createCollectionType();
        
        // Do initial population of results based on networkId filter.
        if (networkId != null && !networkId.isEmpty()) {
            stps.getStp().addAll(nsiTopology.getStpsByNetworkId(networkId));
        }
        else {
            stps.getStp().addAll(nsiTopology.getStps());
        }
        
        // Now we remove based on the remaining filters.
        if (serviceType != null && !serviceType.isEmpty()) {
            for (Iterator<StpType> iter = stps.getStp().iterator(); iter.hasNext();) {
                StpType stp = iter.next();
                
                // Check each STP in the result set to see if it matches the serviceType.
                boolean found = false;
                for (ResourceRefType serviceRef : stp.getServiceType()) {
                    if (serviceType.contentEquals(serviceRef.getType())) {
                        found = true;
                    }
                }
                
                // If we didn't find a matching serviceType in this STP we do not return it.
                if (!found) {
                    iter.remove();
                }
            } 
        }
        
        if (labelType != null && !labelType.isEmpty()) {
            for (Iterator<StpType> iter = stps.getStp().iterator(); iter.hasNext();) {
                StpType stp = iter.next();
                if (!stp.getLabel().getLabeltype().contentEquals(labelType)) {
                    iter.remove();
                }
                else if (labelValue != null && !labelValue.isEmpty()) {
                    if (!labelValue.contentEquals(stp.getLabel().getValue())) {
                        iter.remove();
                    }
                }
            }
        }
        else if (labelValue != null && !labelValue.isEmpty()) {
            // We do not allow a filter of label value without a label type.
            log.error("getStps: Query filter on labelValue must first contain labelType.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).entity("Query filter on labelValue must contain labelType.").build()); 
        }
        
        return nsiFactory.createCollection(stps);
    }
    
    @GET
    @Path("/stps/{stpId}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public JAXBElement<StpType> getStp(@PathParam("stpId") String stpId) {
        // Verify we have the stpId from the request path.  Not sure if this
        // would ever happen.
        if (stpId == null || stpId.isEmpty()) {
            log.error("getStp: Path parameter stpId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter stpId must be provided.").build());
        }
        // Get a reference to topology provider and get the NSI Topology model.
        TopologyProvider topologyProvider = ConfigurationManager.getTopologyProvider();
        NsiTopology nsiTopology = topologyProvider.getTopology();
        
        // Try to locate the requested STP.
        StpType stp = nsiTopology.getStp(stpId);
        if (stp == null) {
            log.error("getStp: Requested stpId does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested stpId does not exist.").build());            
        }
        
        ObjectFactory nsiFactory = new ObjectFactory();
        return nsiFactory.createStp(stp);
    }
}
