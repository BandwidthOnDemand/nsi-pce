package net.es.nsi.pce.topology.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import net.es.nsi.pce.gson.JsonProxy;
import net.es.nsi.pce.spring.SpringContext;
import net.es.nsi.pce.topology.dao.TopologyConfiguration;
import net.es.nsi.pce.topology.jaxb.CollectionType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceAdaptationType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.model.NsiNetworkFactory;
import net.es.nsi.pce.topology.model.NsiNsaFactory;
import net.es.nsi.pce.topology.model.NsiSdpFactory;
import net.es.nsi.pce.topology.model.NsiServiceAdaptationFactory;
import net.es.nsi.pce.topology.model.NsiServiceDomainFactory;
import net.es.nsi.pce.topology.model.NsiServiceFactory;
import net.es.nsi.pce.topology.model.NsiStpFactory;
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
@Path("/topology")
public class TopologyService {
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
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getAllTopology(
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Check to see if there was any change in consolidated topology before
        // looking at individual entries.  This should save some processing.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty() &&
            DateUtils.parseDate(ifModifiedSince).getTime() >= nsiTopology.getLastDiscovered()) {
            return Response.notModified().header("Last-Modified", ifModifiedSince).build();
        }

        // We are stuffing the results into a collection object.
        ObjectFactory nsiFactory = new ObjectFactory();
        CollectionType collection = nsiFactory.createCollectionType();

        // Do a population of the entire network model and we will remove those
        // components not meeting the If-Modified-Since criteria.
        collection.getNetwork().addAll(nsiTopology.getNetworks());
        collection.getNsa().addAll(nsiTopology.getNsas());
        collection.getSdp().addAll(nsiTopology.getSdps());
        collection.getService().addAll(nsiTopology.getServices());
        collection.getServiceAdaptation().addAll(nsiTopology.getServiceAdaptations());
        collection.getServiceDomain().addAll(nsiTopology.getServiceDomains());
        collection.getStp().addAll(nsiTopology.getStps());

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            NsiNetworkFactory.ifModifiedSince(ifModifiedSince, collection.getNetwork());
            NsiNsaFactory.ifModifiedSince(ifModifiedSince, collection.getNsa());
            NsiSdpFactory.ifModifiedSince(ifModifiedSince, collection.getSdp());
            NsiServiceFactory.ifModifiedSince(ifModifiedSince, collection.getService());
            NsiServiceAdaptationFactory.ifModifiedSince(ifModifiedSince, collection.getServiceAdaptation());
            NsiServiceDomainFactory.ifModifiedSince(ifModifiedSince, collection.getServiceDomain());
            NsiStpFactory.ifModifiedSince(ifModifiedSince, collection.getStp());

            // If no STP then return a 304 to indicate no modifications.
            if (collection.getNetwork().isEmpty() &&
                    collection.getNsa().isEmpty() &&
                    collection.getSdp().isEmpty() &&
                    collection.getService().isEmpty() &&
                    collection.getServiceAdaptation().isEmpty() &&
                    collection.getServiceDomain().isEmpty() &&
                    collection.getStp().isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(collection)).build();
    }

    @GET
    @Path("/stps")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getStps(
            @QueryParam("networkId") String networkId,
            @QueryParam("serviceDomain") String serviceDomain,
            @QueryParam("labelType") String labelType,
            @QueryParam("labelValue") String labelValue,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
        log.debug("getStps: networkId=" + networkId + ", serviceDomain=" + serviceDomain + ", labelType=" + labelType + ", labelValue=" + labelValue);

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into aa array.
        List<StpType> stps = new ArrayList<>();

        // Do initial population of results based on networkId filter.
        if (networkId != null && !networkId.isEmpty()) {
            log.debug("getStps: filtering on " + networkId);
            stps.addAll(nsiTopology.getStpsByNetworkId(networkId));
        }
        else {
            stps.addAll(nsiTopology.getStps());
        }

        // Now we remove based on the remaining filters.
        if (serviceDomain != null && !serviceDomain.isEmpty()) {
            for (Iterator<StpType> iter = stps.iterator(); iter.hasNext();) {
                StpType stp = iter.next();

                // Check each STP in the result set to see if it matches the serviceType.
                boolean found = false;
                if (stp.getServiceDomain() != null && stp.getServiceDomain().getId() != null &&
                        serviceDomain.contentEquals(stp.getServiceDomain().getId())) {
                    found = true;
                }

                // If we didn't find a matching serviceType in this STP we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        if (labelType != null && !labelType.isEmpty()) {
            for (Iterator<StpType> iter = stps.iterator(); iter.hasNext();) {
                StpType stp = iter.next();
                if (!stp.getLabel().getType().contentEquals(labelType)) {
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

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<StpType> iter = stps.iterator(); iter.hasNext();) {
                StpType stp = iter.next();
                if (!(modified.compare(stp.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no STP then return a 304 to indicate no modifications.
            if (stps.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(stps, StpType.class)).build();
    }

    @GET
    @Path("/stps/{stpId}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getStp(
            @PathParam("stpId") String stpId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
        log.debug("getStp: id=" + stpId);

        // Verify we have the stpId from the request path.  Not sure if this
        // would ever happen.
        if (stpId == null || stpId.isEmpty()) {
            log.error("getStp: Path parameter stpId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter stpId must be provided.").build());
        }
        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested STP.
        StpType stp = nsiTopology.getStp(stpId);
        if (stp == null) {
            log.error("getStp: Requested stpId does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested stpId does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(stp.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(stp)).build();
    }

    @GET
    @Path("/networks")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getNetworks(
            @QueryParam("nsaId") String nsaId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into an array.
        List<NetworkType> networks = new ArrayList<>();;

        // Do initial population of results based on NSA filter.
        if (nsaId != null && !nsaId.isEmpty()) {
            Collection<NetworkType> networksByNsaId = nsiTopology.getNetworksByNsaId(nsaId);

            if (networksByNsaId != null && !networksByNsaId.isEmpty()) {
                networks.addAll(nsiTopology.getNetworksByNsaId(nsaId));
            }
        }
        else {
            networks.addAll(nsiTopology.getNetworks());
        }

        log.debug("getNetworks: networks after initial filter = " + networks.size());

        // Now we remove based on the remaining filters.
        if (serviceType != null && !serviceType.isEmpty()) {
            for (Iterator<NetworkType> iter = networks.iterator(); iter.hasNext();) {
                NetworkType network = iter.next();

                // Check each STP in the result set to see if it matches the serviceType.
                boolean found = false;
                for (ResourceRefType serviceRef : network.getService()) {
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

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.  TODO: Validate the Network discovered value is populated.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<NetworkType> iter = networks.iterator(); iter.hasNext();) {
                NetworkType network = iter.next();
                if (!(modified.compare(network.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no Networks then return a 304 to indicate no modifications.
            if (networks.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        log.debug("getNetworks: networks returning = " + networks.size());

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(networks, NetworkType.class)).build();
    }

    @GET
    @Path("/networks/{networkId}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getNetwork(
            @PathParam("networkId") String networkId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (networkId == null || networkId.isEmpty()) {
            log.error("getNetwork: Path parameter networkId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter networkId must be provided.").build());
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested Network.
        NetworkType network = nsiTopology.getNetwork(networkId);
        if (network == null) {
            log.error("getNetwork: Requested networkId does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested networkId does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(network.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(network)).build();
    }

    @GET
    @Path("/networks/{networkId}/stps")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getStpsByNetwork(
            @PathParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @QueryParam("labelType") String labelType,
            @QueryParam("labelValue") String labelValue,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (networkId == null || networkId.isEmpty()) {
            log.error("getStpsByNetwork: Path parameter networkId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter networkId must be provided.").build());
        }

        return getStps(networkId, serviceType, labelType, labelValue, ifModifiedSince);
    }

    @GET
    @Path("/nsas")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getNsas(
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into a list object.
        List<NsaType> nsas = new ArrayList<>();

        // Do initial population of all NSA.
        nsas.addAll(nsiTopology.getNsas());

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
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

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(nsas, NsaType.class)).build();
    }

    @GET
    @Path("/nsas/{nsaId}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getNsa(
            @PathParam("nsaId") String nsaId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (nsaId == null || nsaId.isEmpty()) {
            log.error("getNsa: Path parameter nsaId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter nsaId must be provided.").build());
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested Network.
        NsaType nsa = nsiTopology.getNsa(nsaId);
        if (nsa == null) {
            log.error("getNsa: Requested nsaId does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested nsaId does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(nsa.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(nsa)).build();
    }

    @GET
    @Path("/serviceDomains")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServiceDomains(
            @QueryParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into an array.
        List<ServiceDomainType> serviceDomains = new ArrayList<>();

        // Do initial population of all NSA.
        serviceDomains.addAll(nsiTopology.getServiceDomains());

        // Now we remove based on the remaining filters.
        if (networkId != null && !networkId.isEmpty()) {
            for (Iterator<ServiceDomainType> iter = serviceDomains.iterator(); iter.hasNext();) {
                ServiceDomainType serviceDomain = iter.next();

                // Check each serviceDomain in the result set to see if it
                // matches the serviceType.
                boolean found = false;
                if (networkId.contentEquals(serviceDomain.getNetwork().getId())) {
                    found = true;
                }

                // If we didn't find a matching networkId in this serviceDomain
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        // Now we remove based on the remaining filters.
        if (serviceType != null && !serviceType.isEmpty()) {
            for (Iterator<ServiceDomainType> iter = serviceDomains.iterator(); iter.hasNext();) {
                ServiceDomainType serviceDomain = iter.next();

                // Check each serviceDomain in the result set to see if it matches the serviceType.
                boolean found = false;
                if (serviceType.contentEquals(serviceDomain.getService().getType())) {
                    found = true;
                }

                // If we didn't find a matching serviceType in this serviceDomain
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<ServiceDomainType> iter = serviceDomains.iterator(); iter.hasNext();) {
                ServiceDomainType serviceDomain = iter.next();
                if (!(modified.compare(serviceDomain.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no serviceDomain then return a 304 to indicate no modifications.
            if (serviceDomains.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(serviceDomains, ServiceDomainType.class)).build();
    }

    @GET
    @Path("/serviceDomains/{id}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServiceDomain(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (id == null || id.isEmpty()) {
            log.error("getServiceDomain: Path parameter serviceDomain Id must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter serviceDomain Id must be provided.").build());
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested Network.
        ServiceDomainType serviceDomain = nsiTopology.getServiceDomain(id);
        if (serviceDomain == null) {
            log.error("getServiceDomain: Requested serviceDomain id does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested serviceDomain id does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(serviceDomain.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        ObjectFactory nsiFactory = new ObjectFactory();

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(serviceDomain)).build();
    }

    @GET
    @Path("/networks/{networkId}/serviceDomains")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServiceDomainsByNetwork(
            @PathParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (networkId == null || networkId.isEmpty()) {
            log.error("getServiceDomainsByNetwork: Path parameter networkId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter networkId must be provided.").build());
        }

        return getServiceDomains(networkId, serviceType, ifModifiedSince);
    }

    @GET
    @Path("/serviceAdaptations")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServiceAdaptations(
            @QueryParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into an array.
        List<ServiceAdaptationType> serviceAdaptations = new ArrayList<>();

        // Do initial population of all serviceAdaptations.
        serviceAdaptations.addAll(nsiTopology.getServiceAdaptations());

        // Now we remove based on the remaining filters.
        if (networkId != null && !networkId.isEmpty()) {
            for (Iterator<ServiceAdaptationType> iter = serviceAdaptations.iterator(); iter.hasNext();) {
                ServiceAdaptationType serviceAdaptation = iter.next();

                // Check each serviceAdaptation in the result set to see if it
                // matches the networkId.
                boolean found = false;
                if (networkId.contentEquals(serviceAdaptation.getNetwork().getId())) {
                    found = true;
                }

                // If we did not find a matching networkId in this serviceAdaptation
                // we remove it from the result set.
                if (!found) {
                    iter.remove();
                }
            }
        }

        // Now we remove based on the remaining filters.
        if (serviceType != null && !serviceType.isEmpty()) {
            for (Iterator<ServiceAdaptationType> iter = serviceAdaptations.iterator(); iter.hasNext();) {
                ServiceAdaptationType serviceAdaptation = iter.next();

                // Check each serviceAdaptation in the result set to see if it
                // matches the serviceType.
                boolean found = false;
                if (serviceType.contentEquals(serviceAdaptation.getService().getType())) {
                    found = true;
                }

                // If we did not find a matching serviceType in this serviceAdaptation
                // we remove it from the result set.
                if (!found) {
                    iter.remove();
                }
            }
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<ServiceAdaptationType> iter = serviceAdaptations.iterator(); iter.hasNext();) {
                ServiceAdaptationType serviceAdaptation = iter.next();
                if (!(modified.compare(serviceAdaptation.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no serviceDomain then return a 304 to indicate no modifications.
            if (serviceAdaptations.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(serviceAdaptations, ServiceAdaptationType.class)).build();
    }

    @GET
    @Path("/serviceAdaptations/{id}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServiceAdaptation(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the serviceAdaptation Id from the request path.  Not
        // sure if this would ever happen.
        if (id == null || id.isEmpty()) {
            log.error("getServiceAdaptation: Path parameter serviceAdaptation Id must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter serviceAdaptation Id must be provided.").build());
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested serviceAdaptation.
        ServiceAdaptationType serviceAdaptation = nsiTopology.getServiceAdaptation(id);
        if (serviceAdaptation == null) {
            log.error("getServiceAdaptation: Requested serviceAdaptation id does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested serviceAdaptation id does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(serviceAdaptation.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        ObjectFactory nsiFactory = new ObjectFactory();

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(serviceAdaptation)).build();
    }

    @GET
    @Path("/networks/{networkId}/serviceAdaptations")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServiceAdaptationsByNetwork(
            @PathParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (networkId == null || networkId.isEmpty()) {
            log.error("getServiceAdaptationsByNetwork: Path parameter networkId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter networkId must be provided.").build());
        }

        return getServiceAdaptations(networkId, serviceType, ifModifiedSince);
    }

    @GET
    @Path("/services")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServices(
            @QueryParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into an array.
        List<ServiceType> services = new ArrayList<>();

        // Do initial population of all Services.
        services.addAll(nsiTopology.getServices());

        // Now we remove based on the remaining filters.
        if (networkId != null && !networkId.isEmpty()) {
            for (Iterator<ServiceType> iter = services.iterator(); iter.hasNext();) {
                ServiceType service = iter.next();

                // Check each service in the result set to see if it matches
                // the networkiId.
                boolean found = false;
                if (networkId.contentEquals(service.getNetwork().getId())) {
                    found = true;
                }

                // If we didn't find a matching networkId in this service
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        // Now we remove based on the remaining filters.
        if (serviceType != null && !serviceType.isEmpty()) {
            for (Iterator<ServiceType> iter = services.iterator(); iter.hasNext();) {
                ServiceType service = iter.next();

                // Check each service in the result set to see if it matches
                // the serviceType.
                boolean found = false;
                if (serviceType.contentEquals(service.getType())) {
                    found = true;
                }

                // If we didn't find a matching serviceType in this service
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<ServiceType> iter = services.iterator(); iter.hasNext();) {
                ServiceType service = iter.next();
                if (!(modified.compare(service.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no serviceDomain then return a 304 to indicate no modifications.
            if (services.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(services, ServiceType.class)).build();
    }

    @GET
    @Path("/services/{id}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getService(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the service Id from the request path.  Not sure if
        // this would ever happen.
        if (id == null || id.isEmpty()) {
            log.error("getService: Path parameter service Id must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter service Id must be provided.").build());
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested Network.
        ServiceType service = nsiTopology.getService(id);
        if (service == null) {
            log.error("getService: Requested service id does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested service id does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(service.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        ObjectFactory nsiFactory = new ObjectFactory();

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(service)).build();
    }

    @GET
    @Path("/networks/{networkId}/services")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getServicesByNetwork(
            @PathParam("networkId") String networkId,
            @QueryParam("serviceType") String serviceType,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the networkId from the request path.  Not sure if this
        // would ever happen.
        if (networkId == null || networkId.isEmpty()) {
            log.error("getServiceDomainsByNetwork: Path parameter networkId must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter networkId must be provided.").build());
        }

        return getServices(networkId, serviceType, ifModifiedSince);
    }

    @GET
    @Path("/sdps")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getSdps(
            @QueryParam("networkId") String networkId,
            @QueryParam("serviceDomainId") String serviceDomainId,
            @QueryParam("serviceType") String serviceType,
            @QueryParam("stpId") String stpId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // We are stuffing the results into a collection object.
        List<SdpType> sdps = new ArrayList<>();

        // Do initial population of Sdps.
        if (stpId != null && !stpId.isEmpty()) {
            sdps.addAll(nsiTopology.getSdpMember(stpId));
        }
        else {
            sdps.addAll(nsiTopology.getSdps());
        }

        // Now we remove based on Network Identifier.
        if (networkId != null && !networkId.isEmpty()) {
            for (Iterator<SdpType> iter = sdps.iterator(); iter.hasNext();) {
                SdpType sdp = iter.next();

                // Check each sdp in the result set to see if it matches
                // the networkId.
                boolean found = false;
                if (networkId.contentEquals(sdp.getDemarcationA().getNetwork().getId()) ||
                        networkId.contentEquals(sdp.getDemarcationZ().getNetwork().getId())) {
                    found = true;
                }

                // If we didn't find a matching networkId in this service
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        // Now we remove based on ServiceDomain identifier.
        if (serviceDomainId != null && !serviceDomainId.isEmpty()) {
            for (Iterator<SdpType> iter = sdps.iterator(); iter.hasNext();) {
                SdpType sdp = iter.next();

                // Check each sdp in the result set to see if it matches
                // the networkId.
                boolean found = false;
                if (serviceDomainId.contentEquals(sdp.getDemarcationA().getServiceDomain().getId()) ||
                        serviceDomainId.contentEquals(sdp.getDemarcationZ().getServiceDomain().getId())) {
                    found = true;
                }

                // If we didn't find a matching networkId in this service
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        // Now we remove based on ServiceType.
        if (serviceType != null && !serviceType.isEmpty()) {
            for (Iterator<SdpType> iter = sdps.iterator(); iter.hasNext();) {
                SdpType sdp = iter.next();

                // Check each SDP in the result set to see if it matches
                // the serviceType.
                boolean found = false;
                if (serviceType.contentEquals(sdp.getDemarcationA().getServiceDomain().getType())) {
                    found = true;
                }

                // If we didn't find a matching serviceType in this SDP
                // we do not return it.
                if (!found) {
                    iter.remove();
                }
            }
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<SdpType> iter = sdps.iterator(); iter.hasNext();) {
                SdpType sdp = iter.next();
                if (!(modified.compare(sdp.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no serviceDomain then return a 304 to indicate no modifications.
            if (sdps.isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serializeList(sdps, SdpType.class)).build();
    }

    @GET
    @Path("/sdps/{id}")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response getSdp(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        // Verify we have the service Id from the request path.  Not sure if
        // this would ever happen.
        if (id == null || id.isEmpty()) {
            log.error("getService: Path parameter SDP Id must be provided.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Path parameter SDP Id must be provided.").build());
        }

        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();

        // Try to locate the requested Network.
        SdpType sdp = nsiTopology.getSdp(id);
        if (sdp == null) {
            log.error("getSdp: Requested SDP id does not exist.");
            throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity("Requested SDP id does not exist.").build());
        }

        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (!(modified.compare(sdp.getDiscovered()) == DatatypeConstants.LESSER)) {
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        ObjectFactory nsiFactory = new ObjectFactory();

        // Just a 200 response.
        return Response.ok().header("Last-Modified", date).entity(proxy.serialize(sdp)).build();
    }

    /*****************************************************
     * OAM related REST interfaces for Topology service.
     *****************************************************/

    /**
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON, "application/vnd.org.ogf.nsi.topology.v1+json" })
    public Response ping() throws Exception {
        // Get a reference to topology provider and get the NSI Topology model.
        NsiTopology nsiTopology = topologyProvider.getTopology();
        String date = DateUtils.formatDate(new Date(nsiTopology.getLastDiscovered()), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).build();
    }
}
