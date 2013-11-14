package net.es.nsi.pce.pf;

import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import net.es.nsi.pce.api.jaxb.AuthObjectType;
import net.es.nsi.pce.api.jaxb.EthernetBaseType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.api.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.api.jaxb.ResolvedPathType;
import net.es.nsi.pce.api.jaxb.StpType;
import net.es.nsi.pce.services.Point2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles requests for path finding from the RESTful web service.
 * Parameters are validated as much as possible before issuing to the specific
 * path find algorithms.
 * @author hacksaw
 */
public class PathfinderCore {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationContext context = null;
    private ServiceInfoProvider sip = null;
    private AuthProvider ap = null;
    private TopologyProvider topologyProvider = null;
    
    /**
     * Default constructor for this class.  Performs lookups on the Spring
     * beans for required services.
     * 
     * @throws Exception If Spring cannot resolve the desired beans.
     */
    public PathfinderCore() throws Exception {
        // Load the providers we will need for Path Computation.
        SpringContext sc  = SpringContext.getInstance();
        context = sc.getContext();
        sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
        ap = (AuthProvider) context.getBean("authProvider");
        topologyProvider = (TopologyProvider) context.getBean("topologyProvider");       
    }
    
    /**
     * This class builds a path finding request, loads the appropriate path
     * finding modules, issues the path finding request, collects the
     * results, and builds a list of NSA and request segments.
     * 
     * @param serviceType
     * @param p2ps 
     * @param algorithm
     * @param po
     * @return
     * @throws Exception 
     */
    public List<ResolvedPathType> findPath(String serviceType, P2PServiceBaseType p2ps, FindPathAlgorithmType algorithm, List<ResolvedPathType> po) throws Exception {
        log.debug("findPath: EVTS path request");

        // Build the path computation constraints for this request.
        PCEData pceData = new PCEData();
        
        pceData.getConstraints().addAll(Point2Point.getConstraints(p2ps));

        pceData.setTopology(topologyProvider.getTopology());
      
        PCEModule pce;
        if (algorithm == null) {
            pce = (PCEModule) context.getBean("chainPCE");

        } else if (algorithm.equals(FindPathAlgorithmType.CHAIN)) {
            pce = (PCEModule) context.getBean("chainPCE");

        } else if (algorithm.equals(FindPathAlgorithmType.TREE)) {
            pce = (PCEModule) context.getBean("treePCE");
        } else {
            pce = (PCEModule) context.getBean("chainPCE");
        }

        // Invoke the path computation sequence on this request.
        PCEData result = pce.apply(pceData);

        // TODO: Need to better model errors.
        if (result == null) {
            throw new Exception("null result");
        } else if (result.getPath() == null) {

            throw new Exception("null path");
        }

        // For each pair of STP we need to build a resolved path.
        for (StpPair stpPair: result.getPath().getStpPairs() ) {
            // Build the resulting STP objects for this path segment.
            String networkId = stpPair.getA().getNetworkId();
            StpType aStpObj = new StpType();
            aStpObj.setLocalId(stpPair.getA().getLocalId());
            aStpObj.setNetworkId(networkId);
            StpType zStpObj = new StpType();
            zStpObj.setLocalId(stpPair.getZ().getLocalId());
            zStpObj.setNetworkId(networkId);

            // Build our path finding results into an EVTS service.
            P2PServiceBaseType p2psResult = Point2Point.createType(p2ps.getClass());
            p2psResult.setSourceSTP(aStpObj);
            p2psResult.setDestSTP(zStpObj);

            // TODO: These are copied directly from request for now.
            p2psResult.setCapacity(p2ps.getCapacity());
            p2psResult.setDirectionality(p2ps.getDirectionality());
            p2psResult.setSymmetricPath(p2ps.isSymmetricPath());
            
            if (p2ps instanceof EthernetBaseType) {
                EthernetBaseType ets = (EthernetBaseType) p2ps;
                EthernetBaseType etsResult = (EthernetBaseType) p2psResult;
                etsResult.setMtu(ets.getMtu());
                etsResult.setBurstsize(ets.getBurstsize());                
            }
            
            if (p2ps instanceof EthernetVlanType) {
                EthernetVlanType evts = (EthernetVlanType) p2ps;
                EthernetVlanType evtsResult = (EthernetVlanType) p2psResult;
                evtsResult.setSourceVLAN(evts.getSourceVLAN());
                evtsResult.setDestVLAN(evts.getDestVLAN());
            }
            
            // Set the corresponding serviceType and add out EVTS results.
            ResolvedPathType pathObj = new ResolvedPathType();
            pathObj.setServiceType(serviceType);
            pathObj.getAny().add(p2psResult);
            
            /* Look up the managing NSA for this path segment.  TODO: This
             * should use control plane topology to determine the next NSA to
             * pass the request just in case we cannot talk to the managing NSA
             * directly.
             */
            ServiceInfo si = sip.byNetworkId(networkId);
            pathObj.setNsa(si.getNsaId());
            pathObj.setCsProviderURL(si.getProviderUrl());

            // Get the Auth mechanism and credentials for this NSA.
            AuthObjectType ao = new AuthObjectType();
            Map<AuthCredential, String> credentials = ap.getCredentials(si.getNsaId());
            ao.setMethod(ap.getMethod(si.getNsaId()));

            if (credentials.containsKey(AuthCredential.TOKEN)) {
                ao.setToken(credentials.get(AuthCredential.TOKEN));
            }
            if (credentials.containsKey(AuthCredential.USERNAME)) {
                ao.setUsername(credentials.get(AuthCredential.USERNAME));
            }

            if (credentials.containsKey(AuthCredential.PASSWORD)) {
                ao.setPassword(credentials.get(AuthCredential.PASSWORD));
            }

            pathObj.setCredentials(ao);

            po.add(pathObj);
        }

        return po;
    }
}