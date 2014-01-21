/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.api;

import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.Utilities;
import net.es.nsi.pce.logs.PceLogger;
import net.es.nsi.pce.managemenet.jaxb.LogType;
import net.es.nsi.pce.managemenet.jaxb.LogsType;
import net.es.nsi.pce.managemenet.jaxb.StatusType;
import net.es.nsi.pce.managemenet.jaxb.TopologyProviderType;
import net.es.nsi.pce.managemenet.jaxb.TopologyStatusType;
import net.es.nsi.pce.managemenet.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import net.es.nsi.pce.topology.provider.TopologyProviderStatus;
import net.es.nsi.pce.topology.provider.TopologyStatus;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/management")
public class ManagementService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * 
     * @return
     * @throws Exception 
     */
    @GET
    @Path("/status")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getStatus() throws Exception {
        // Get a reference to topology provider and get the NSI Topology model.
        TopologyProvider topologyProvider = ConfigurationManager.getTopologyProvider();
        
        ObjectFactory managementFactory = new ObjectFactory();
        
        // Get the overall topology provider status.
        TopologyStatusType topologyStatus = managementFactory.createTopologyStatusType();
        TopologyStatus providerStatus = topologyProvider.getSummaryStatus();
        topologyStatus.setCode(providerStatus.getCode());
        topologyStatus.setLabel(providerStatus.getLabel());
        topologyStatus.setDescription(providerStatus.getDescription());
        
        // Create and populate the status element to return in response.
        StatusType status = managementFactory.createStatusType();
        status.setStatus(topologyStatus);
        status.setAuditInterval(topologyProvider.getAuditInterval());
        status.setLastAudit(Utilities.longToXMLGregorianCalendar(topologyProvider.getLastAudit()));
        status.setLastModified(Utilities.longToXMLGregorianCalendar(topologyProvider.getLastModified()));
        
        // Populate the manifest status if available.
        TopologyProviderStatus manifestStatus = topologyProvider.getManifestStatus();
        if (manifestStatus != null) {
            TopologyProviderType manifest = managementFactory.createTopologyProviderType();
            manifest.setId(manifestStatus.getId());
            manifest.setHref(manifestStatus.getHref());
            
            TopologyStatusType manStatus = managementFactory.createTopologyStatusType();
            TopologyStatus stat = manifestStatus.getStatus();
            manStatus.setCode(stat.getCode());
            manStatus.setLabel(stat.getLabel());
            manStatus.setDescription(stat.getDescription());
            manifest.setStatus(manStatus);
            
            manifest.setLastAudit(Utilities.longToXMLGregorianCalendar(manifestStatus.getLastAudit()));
            manifest.setLastSuccessfulAudit(Utilities.longToXMLGregorianCalendar(manifestStatus.getLastSuccessfulAudit()));
            manifest.setLastModified(Utilities.longToXMLGregorianCalendar(manifestStatus.getLastModified()));
            manifest.setLastDiscovered(Utilities.longToXMLGregorianCalendar(manifestStatus.getLastDiscovered()));
            
            status.setManifest(manifest);
        }
        
        // Populate the individual topology providers status.
        Collection<TopologyProviderStatus> provstat = topologyProvider.getProviderStatus();
        for (TopologyProviderStatus ps : provstat) {
            TopologyProviderType provider = managementFactory.createTopologyProviderType();
            provider.setId(ps.getId());
            provider.setHref(ps.getHref());
            
            TopologyStatusType provStatus = managementFactory.createTopologyStatusType();
            TopologyStatus stat = ps.getStatus();
            provStatus.setCode(stat.getCode());
            provStatus.setLabel(stat.getLabel());
            provStatus.setDescription(stat.getDescription());
            provider.setStatus(provStatus);
            
            provider.setLastAudit(Utilities.longToXMLGregorianCalendar(ps.getLastAudit()));
            provider.setLastSuccessfulAudit(Utilities.longToXMLGregorianCalendar(ps.getLastSuccessfulAudit()));
            provider.setLastModified(Utilities.longToXMLGregorianCalendar(ps.getLastModified()));
            provider.setLastDiscovered(Utilities.longToXMLGregorianCalendar(ps.getLastDiscovered()));
            
            status.getProvider().add(provider);      
        }
        
        String date = DateUtils.formatDate(new Date(topologyProvider.getLastAudit()), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<StatusType>>(managementFactory.createStatus(status)) {}).build();
    }
    
    @GET
    @Path("/logs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getLogs(@HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
        // Get a reference to the topology log manager.
        PceLogger topologyLogger = PceLogger.getLogger();
        
        ObjectFactory managementFactory = new ObjectFactory();
        
        // Get the overall topology provider status.
        LogsType topologylogs = managementFactory.createLogsType();
        Collection<LogType> logs = topologyLogger.getLogs();
        topologylogs.getLog().addAll(logs);
        
        String date = DateUtils.formatDate(new Date(topologyLogger.getLastlogTime()), DateUtils.PATTERN_RFC1123);
                
        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            
            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!(modified.compare(result.getDate()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
            
            // If no serviceDomain then return a 304 to indicate no modifications.
            if (topologylogs.getLog().isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<LogsType>>(managementFactory.createLogs(topologylogs)) {}).build();
    }
    
    @GET
    @Path("/logs/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getLog(
            @PathParam("id") String id,
            @PathParam("audit") String audit) throws Exception {
        
        // Verify we have the service Id from the request path.  Not sure if
        // this would ever happen.
        if (id == null || id.isEmpty()) {
            log.error("getLog: Path log Id must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Log parameter log Id must be provided.").build());
        }
        
        // Get a reference to the topology log manager.
        PceLogger topologyLogger = PceLogger.getLogger();
        
        ObjectFactory managementFactory = new ObjectFactory();
        
        // Try to locate the requested Network.
        LogType result = topologyLogger.getLog(id);
        if (result == null) {
            log.error("getLog: Requested log id does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested log id does not exist.").build());            
        }

        // Just a 200 response.
        return Response.ok().entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(result)) {}).build();
    }    
}
