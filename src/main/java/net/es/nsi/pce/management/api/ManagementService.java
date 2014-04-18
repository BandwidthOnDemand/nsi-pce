package net.es.nsi.pce.management.api;

import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.management.logs.PceLogs;
import net.es.nsi.pce.management.jaxb.LogEnumType;
import net.es.nsi.pce.management.jaxb.LogListType;
import net.es.nsi.pce.management.jaxb.LogType;
import net.es.nsi.pce.management.jaxb.StatusType;
import net.es.nsi.pce.management.jaxb.TopologyProviderType;
import net.es.nsi.pce.management.jaxb.ObjectFactory;
import net.es.nsi.pce.management.jaxb.TimerListType;
import net.es.nsi.pce.management.jaxb.TimerStatusType;
import net.es.nsi.pce.management.jaxb.TimerType;
import net.es.nsi.pce.sched.PCEScheduler;
import net.es.nsi.pce.sched.SchedulerItem;
import net.es.nsi.pce.schema.XmlUtilities;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import net.es.nsi.pce.topology.provider.TopologyProviderStatus;
import org.apache.http.client.utils.DateUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation class for the REST-based management interface.
 * 
 * @author hacksaw
 */
@Path("/management")
public class ManagementService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PceLogger pceLogger = PceLogger.getLogger();
    private final ObjectFactory managementFactory = new ObjectFactory();
    
    /**
     * Returns the current topology audit status.
     * 
     * @return Current topology audit status.
     * @throws Exception If there was an internal error.
     */
    @GET
    @Path("/status/topology")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getTopologyAuditStatus() throws Exception {
        // Get a reference to topology provider and get the NSI Topology model.
        TopologyProvider topologyProvider = ConfigurationManager.INSTANCE.getTopologyProvider();
        
        // Create and populate the status element to return in response.
        StatusType status = managementFactory.createStatusType();
        status.setStatus(topologyProvider.getAuditStatus());
        status.setAuditInterval(topologyProvider.getAuditInterval());
        status.setLastAudit(XmlUtilities.longToXMLGregorianCalendar(topologyProvider.getLastAudit()));
        status.setLastDiscovered(XmlUtilities.longToXMLGregorianCalendar(topologyProvider.getLastDiscovered()));
        
        // Populate the provider status if available.
        TopologyProviderStatus providerStatus = topologyProvider.getProviderStatus();
        if (providerStatus != null) {
            TopologyProviderType provider = managementFactory.createTopologyProviderType();
            provider.setId(providerStatus.getId());
            provider.setHref(providerStatus.getHref());
            provider.setStatus(providerStatus.getStatus());           
            provider.setLastAudit(XmlUtilities.longToXMLGregorianCalendar(providerStatus.getLastAudit()));
            provider.setLastSuccessfulAudit(XmlUtilities.longToXMLGregorianCalendar(providerStatus.getLastSuccessfulAudit()));
            provider.setLastDiscovered(XmlUtilities.longToXMLGregorianCalendar(providerStatus.getLastDiscovered()));
            
            status.setProvider(provider);
        }
        
        String date = DateUtils.formatDate(new Date(topologyProvider.getLastAudit()), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<StatusType>>(managementFactory.createStatus(status)) {}).build();
    }

    /**
     * Returns a list of all configured timers within the PCE scheduler.
     * 
     * @return List of all configured timers within the PCE scheduler.
     * @throws Exception If there was an internal error during processing.
     */
    @GET
    @Path("/timers")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getTimers() throws Exception {
        Collection<SchedulerItem> list = PCEScheduler.getInstance().list();
        TimerListType timerList = managementFactory.createTimerListType();
        for (SchedulerItem item : list) {
            TimerType timer = managementFactory.createTimerType();
            
            timer.setId(item.getId());
            timer.setHref("/timers/" + item.getId());
            timer.setTimerInterval(item.getInterval());
            
            TimerStatusType currentStatus = PCEScheduler.getInstance().getCurrentStatus(item.getId());
            timer.setTimerStatus(currentStatus);
            
            Date nextRun = PCEScheduler.getInstance().getNextRun(item.getId());
            if (nextRun != null) {
                timer.setNextExecution(XmlUtilities.longToXMLGregorianCalendar(nextRun.getTime()));
            }
            
            timerList.getTimer().add(timer);
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<TimerListType>>(managementFactory.createTimers(timerList)) {}).build();
    }
    
    /**
     * Get the individual timer resource from the PCE scheduler as identified by "id".
     * 
     * @param id The identifier of the timer entry.
     * @return The timer entry.
     * @throws Exception If there was an internal error during processing.
     */
    @GET
    @Path("/timers/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getTimer(@PathParam("id") String id) throws Exception {
        SchedulerItem item = PCEScheduler.getInstance().get(id);
        if (item == null) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }

        TimerType timer = managementFactory.createTimerType();

        timer.setId(item.getId());
        timer.setHref("/timers/" + item.getId());
        timer.setTimerInterval(item.getInterval());

        TimerStatusType currentStatus = PCEScheduler.getInstance().getCurrentStatus(id);
        timer.setTimerStatus(currentStatus);

        Date nextRun = PCEScheduler.getInstance().getNextRun(item.getId());
        if (nextRun != null) {
            timer.setNextExecution(XmlUtilities.longToXMLGregorianCalendar(nextRun.getTime()));
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<TimerType>>(managementFactory.createTimer(timer)) {}).build();
    }
    
    /**
     * Update the interval associated with the timer resource identified by "id".
     * The PUT timer resource should have all the fields of the original timer
     * resource, except for the changed timer interval.
     * 
     * @param id The identifier of the timer entry to update.
     * @param resource The updated timer entry.
     * @return The updated timer entry as committed, or an error.
     * @throws Exception If there was an internal error during processing.
     */
    @PUT
    @Path("/timers/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response updateTimer(@PathParam("id") String id, TimerType resource) throws Exception {
        // Validate a few of the fields to make sure this is the correct resource.
        SchedulerItem item = PCEScheduler.getInstance().get(id);
        if (item == null) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }
        
        if (resource == null) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id, "timer resource missing");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }

        if (!item.getId().equalsIgnoreCase(resource.getId())) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id, "only interval of Timer resource can be modified " + resource.getId());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
        }

        String href = "/timers/" + item.getId();
        if (!href.equalsIgnoreCase(resource.getHref())) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id, "only interval of Timer resource can be modified " + resource.getHref());
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
        }

        if (resource.getTimerInterval() <= 0) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id, "Timer interval must be a positive integer " + resource.getTimerInterval());
            return Response.status(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
        }
        
        // Looks like we are good to process the change request.
        try {
            boolean update = PCEScheduler.getInstance().update(id, resource.getTimerInterval());
        
            if (!update) {
                LogType error = pceLogger.error(PceErrors.MANAGEMENT_TIMER_MODIFICATION, id, "failed");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
            }
        }
        catch (SchedulerException se) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_TIMER_MODIFICATION, id, se.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }
        
        return getTimer(id);
    }
    
    /**
     * Modify the current status of a timer resource.
     * 
     * @param id Identifier of the timer.
     * @return The new status of the timer.
     * @throws Exception If there is an internal server error.
     */
    @PUT
    @Path("/timers/{id}/status")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response updateStatus(@PathParam("id") String id, TimerStatusType timerStatus) throws Exception {
        TimerStatusType currentStatus = PCEScheduler.getInstance().getCurrentStatus(id);
        if (currentStatus != timerStatus) {      
            try {
                if (timerStatus == TimerStatusType.RUNNING) {
                    pceLogger.log(PceLogs.AUDIT_USER, id);
                    PCEScheduler.getInstance().runNow(id);
                }
                else if (timerStatus == TimerStatusType.HAULTED) {
                    pceLogger.log(PceLogs.AUDIT_HAULTED, id);
                    PCEScheduler.getInstance().hault(id);
                }
                else if (timerStatus == TimerStatusType.SCHEDULED) {
                    pceLogger.log(PceLogs.AUDIT_SCHEDULED, id);
                    PCEScheduler.getInstance().schedule(id);
                }
                else {
                    LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id, "Invalid operation type " + timerStatus.value());
                    return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
                }
            } catch (NotFoundException nf) {
                LogType error = pceLogger.error(PceErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
                return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
            } catch (BadRequestException br) {
                LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id);
                return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();                   
            }
        }

        // There can be timing issues with setting a job to RUNNING and the
        // current status so just return the requested status for now.
        return Response.ok().entity(new GenericEntity<JAXBElement<TimerStatusType>>(managementFactory.createTimerStatus(timerStatus)) {}).build();
    }

    /**
     * Get the current status of a timer resource.
     * 
     * @param id Identifier of the timer.
     * @return The status of the timer.
     * @throws Exception If there is an internal server error.
     */
    @GET
    @Path("/timers/{id}/status")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getStatus(@PathParam("id") String id) throws Exception {
        // Validate a few of the fields to make sure this is the correct resource.
        SchedulerItem item = PCEScheduler.getInstance().get(id);
        if (item == null) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }

        TimerStatusType currentStatus;
        try {
            currentStatus = PCEScheduler.getInstance().getCurrentStatus(id);
        } catch (NotFoundException nf) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
        }
            
        return Response.ok().entity(new GenericEntity<JAXBElement<TimerStatusType>>(managementFactory.createTimerStatus(currentStatus)) {}).build();
    }
    
    /**
     * Retrieve a list of logs matching the specified criteria.  All parameters
     * are optional.
     * 
     * @param ifModifiedSince Logs that have occurred since this time.
     * @param type The type of log to retrieve.
     * @param code The code of the log to retrieve.  Can only be supplied when there is a type supplied.
     * @param label The character string label of the logs to retrieve.
     * @param audit Retrieve all logs for the identified audit run.
     * @return The list of logs matching the criteria.
     * @throws Exception If there is an internal server error.
     */
    @GET
    @Path("/logs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getLogs(@HeaderParam("If-Modified-Since") String ifModifiedSince,
            @QueryParam("type") String type, /* One of "Log" or "Error". */
            @QueryParam("code") String code, /* Will convert to an integer. */
            @QueryParam("label") String label,
            @QueryParam("audit") String audit) throws Exception {
        
        // Get the overall topology provider status.
        LogListType topologylogs = managementFactory.createLogListType();
        Collection<LogType> logs = pceLogger.getLogs();
        topologylogs.getLog().addAll(logs);

        // TODO: Linear searches through thousands of logs will get slow.  Fix
        // if it becomes a problem.
        if (type != null && !type.isEmpty()) {
            if (!LogEnumType.LOG.value().equalsIgnoreCase(type) &&
                    !LogEnumType.ERROR.value().equalsIgnoreCase(type)) {
                LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, type, "Invalid log type");
                return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();                
            }
            
            int codeInt = -1;
            if (code != null && !code.isEmpty()) {
                try {
                    codeInt = Integer.parseInt(code);
                }
                catch (NumberFormatException ne) {
                    LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, type, "Invalid code value");
                    return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();                
                }
            }
            
            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!result.getType().value().equalsIgnoreCase(type)) {
                    iter.remove();
                }
                else if (codeInt > -1 && codeInt != result.getCode()) {
                    iter.remove();
                }
            }
        }
        else if (code != null && !code.isEmpty()) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, code, "Code query parameter must be paired with a type parameter");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();            
        }
        
        if (label != null && !label.isEmpty()) {
            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!result.getLabel().equalsIgnoreCase(label)) {
                    iter.remove();
                }
            }            
        }
        
        if (audit != null && !audit.isEmpty()) {
            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                XMLGregorianCalendar auditDate = result.getAudit();
                if (auditDate != null) {
                    if (!auditDate.toXMLFormat().equalsIgnoreCase(audit)) {
                        iter.remove();
                    }
                }
            }            
        }
        
        String date = DateUtils.formatDate(new Date(pceLogger.getLastlogTime()), DateUtils.PATTERN_RFC1123);

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

        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<LogListType>>(managementFactory.createLogs(topologylogs)) {}).build();
    }
    
    /**
     * Get a specific log entry.
     * @param id The identifier of the log.
     * @param audit 
     * @return The log if it exists.
     * @throws Exception If there is an internal server error.
     */
    @GET
    @Path("/logs/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.pce.v1+json", "application/vnd.net.es.pce.v1+xml" })
    public Response getLog(
            @PathParam("id") String id) throws Exception {
        
        // Verify we have the service Id from the request path.  Not sure if
        // this would ever happen.
        if (id == null || id.isEmpty()) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_BAD_REQUEST, id, "Log identifier must be specified in path");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }
        
        // Try to locate the requested Network.
        LogType result = pceLogger.getLog(id);
        if (result == null) {
            LogType error = pceLogger.error(PceErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();          
        }

        // Just a 200 response.
        return Response.ok().entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(result)) {}).build();
    }
}
