/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.description.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.gson.JsonProxy;
import net.es.nsi.pce.spring.SpringContext;
import net.es.nsi.pce.topology.dao.TopologyConfiguration;
import net.es.nsi.pce.jaxb.topology.NsaType;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author hacksaw
 */
@Path("/descriptions")
public class NsaDescriptionService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private TopologyConfiguration configuration;
    private TopologyProvider topologyProvider;
    private JsonProxy proxy;

    @PostConstruct
    public void init() {
        SpringContext sc = SpringContext.getInstance();
        final ApplicationContext applicationContext = sc.getContext();
        configuration = (TopologyConfiguration) applicationContext.getBean("topologyConfiguration");
        topologyProvider = (TopologyProvider) applicationContext.getBean("topologyProvider");
        proxy = (JsonProxy) applicationContext.getBean("jsonProxy");

    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.net.es.description.v1+json" })
    public Response getDescriptions(@HeaderParam("Accept") String accept, @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
        // Make sure we are properly initalized.
        if (configuration == null || topologyProvider == null || proxy == null) {
            Response.serverError().entity("Service not initialized").build();
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into a collection object.
        Collection<NsaType> nsas = new ArrayList<>();

        // Do initial population of all NSA.
        nsas.addAll(nsiTopology.getNsas());

        if (nsas.isEmpty()) {
            log.debug("Empty " + nsiTopology.getLastDiscovered());
        }

        // Now filter by the If-Modified-Since header.  TODO: Validate the Network discovered value is populated.
        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            log.debug("Empty " + nsiTopology.getLastDiscovered());
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<NsaType> iter = nsas.iterator(); iter.hasNext();) {
                NsaType nsa = iter.next();
                if (!(modified.compare(nsa.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no NSA then return a 304 to indicate no modifications.
            if (nsas.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        String response = proxy.serialize(nsas);
        return  Response.ok().entity(response).build();
    }

    /**
     * @return the configuration
     */
    public TopologyConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(TopologyConfiguration configuration) {
        this.configuration = configuration;
    }
}
