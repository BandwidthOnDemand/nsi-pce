/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.route.RouteObject;
import net.es.nsi.pce.pf.simple.SimpleStp;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.path.StpListType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.path.services.Service;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class Request {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PCEData pceData;
    private P2PServiceBaseType p2p;
    private boolean symmetricPath;
    private DirectionalityType directionality;
    private String sourceStp;
    private String destStp;
    private Optional<StpListType> ero;
    private SimpleStp srcStpId;
    private SimpleStp dstStpId;
    private NsiTopology nsiTopology;
    private RouteObject ro;

    public Request(PCEData pceData) throws WebApplicationException {
        // We currently implement P2PS policies in the PCE.  Reject any path
        // finding requests for services we do not understand.
        this.pceData = pceData;

        AttrConstraints constraints = pceData.getAttrConstraints();
        String serviceType = PfUtils.getServiceTypeOrFail(constraints);
        List<Service> serviceByType = Service.getServiceByType(serviceType);
        if (!serviceByType.contains(Service.P2PS)) {
            throw Exceptions.unsupportedParameter(PCEConstraints.NAMESPACE, PCEConstraints.SERVICETYPE, serviceType);
        }

        // Generic reservation information is in string constraint attributes,
        // but the P2PS specific constraints are in the P2PS P2PServiceBaseType.
        p2p = PfUtils.getP2PServiceBaseTypeOrFail(constraints);

        // Determine directionality of service request, default to bidirectional if not present.
        directionality = PfUtils.getDirectionality(p2p);

        // Determine path symmetry.
        symmetricPath = PfUtils.getSymmetricPath(p2p);

        // Get source stpId.
        sourceStp = PfUtils.getSourceStpOrFail(p2p);

        // Get destination stpId.
        destStp = PfUtils.getDestinationStpOrFail(p2p);

        // Get the optional ERO object.
        ero = PfUtils.getEro(p2p);

        // Parse the source STP to make sure it is valid.
        srcStpId = PfUtils.getSimpleStpOrFail(sourceStp);

        // Parse the destination STP to make sure it is valid.
        dstStpId = PfUtils.getSimpleStpOrFail(destStp);

        // Get the topology model used for routing.
        nsiTopology = pceData.getTopology();

        // Log topologies applied to this request.
        log.debug("localId " + nsiTopology.getLocalNsaId());
        nsiTopology.getNetworkIds().stream().forEach((networkId) -> {
            log.debug("networkId " + networkId);
        });

        // Segment the request into subroutes if an ERO is provided.
        ro = new RouteObject(nsiTopology, srcStpId, dstStpId, directionality, ero);
    }

    /**
     * @return the p2p
     */
    public P2PServiceBaseType getP2p() {
        return p2p;
    }

    /**
     * @return the symmetricPath
     */
    public boolean isSymmetricPath() {
        return symmetricPath;
    }

    /**
     * @return the directionality
     */
    public DirectionalityType getDirectionality() {
        return directionality;
    }

    /**
     * @return the sourceStp
     */
    public String getSourceStp() {
        return sourceStp;
    }

    /**
     * @return the destStp
     */
    public String getDestStp() {
        return destStp;
    }

    /**
     * @return the ero
     */
    public Optional<StpListType> getEro() {
        return ero;
    }

    /**
     * @return the srcStpId
     */
    public SimpleStp getSrcStpId() {
        return srcStpId;
    }

    /**
     * @return the dstStpId
     */
    public SimpleStp getDstStpId() {
        return dstStpId;
    }

    /**
     * @return the nsiTopology
     */
    public NsiTopology getNsiTopology() {
        return nsiTopology;
    }

    /**
     * @return the ro
     */
    public RouteObject getRo() {
        return ro;
    }

    /**
     * @return the pceData
     */
    public PCEData getPceData() {
        return pceData;
    }
}
