package net.es.nsi.pce.pf;


import net.es.nsi.pce.config.SpringContext;

import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.config.nsa.auth.AuthCredential;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;

import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import net.es.nsi.pce.api.jaxb.AuthObjectType;
import net.es.nsi.pce.api.jaxb.EthernetBaseType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.FindPathAlgorithmType;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.api.jaxb.ResolvedPathType;
import net.es.nsi.pce.api.jaxb.StpType;
import net.es.nsi.pce.services.Point2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author hacksaw
 */
public class PathfinderCore {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationContext context = null;
    private ServiceInfoProvider sip = null;
    private AuthProvider ap = null;
    private TopologyProvider tp = null;
    
    public PathfinderCore() throws Exception {
        // Load the providers we will need for Path Computation.
        SpringContext sc  = SpringContext.getInstance();
        context = sc.getContext();
        sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
        ap = (AuthProvider) context.getBean("authProvider");
        tp = (TopologyProvider) context.getBean("topologyProvider");
        //tp.loadTopology();        
    }
    
    /**
     * 
     * @param serviceType
     * @param evts
     * @param algorithm
     * @param po
     * @return
     * @throws Exception 
     */
    public List<ResolvedPathType> findPath(String serviceType, EthernetVlanType evts, FindPathAlgorithmType algorithm, List<ResolvedPathType> po) throws Exception {
        log.debug("findPath: EVTS path request");

        // Build the path computation constraints for this request.
        PCEData pceData = new PCEData();
        
        pceData.getConstraints().addAll(Point2Point.getConstraints(evts));

        pceData.setTopology(tp.getTopology());

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
            String networkId = stpPair.getA().getNetwork().getNetworkId();
            StpType aStpObj = new StpType();
            aStpObj.setLocalId(stpPair.getA().getLocalId());
            aStpObj.setNetworkId(networkId);
            StpType zStpObj = new StpType();
            zStpObj.setLocalId(stpPair.getZ().getLocalId());
            zStpObj.setNetworkId(networkId);

            // Build our path finding results into an EVTS service.
            ObjectFactory factory = new ObjectFactory();
            EthernetVlanType evtsResult = factory.createEthernetVlanType();
            evtsResult.setSourceSTP(aStpObj);
            evtsResult.setDestSTP(zStpObj);

            // TODO: These are copied directly from request for now.
            evtsResult.setSourceVLAN(evts.getSourceVLAN());
            evtsResult.setDestVLAN(evts.getDestVLAN());
            evtsResult.setCapacity(evts.getCapacity());
            evtsResult.setBurstsize(evts.getBurstsize());
            evtsResult.setMtu(evts.getMtu());
            evtsResult.setDirectionality(evts.getDirectionality());
            evtsResult.setSymmetricPath(evts.isSymmetricPath());
            
            // Set the corresponding serviceType and add out EVTS results.
            ResolvedPathType pathObj = new ResolvedPathType();
            pathObj.setServiceType(serviceType);
            pathObj.getAny().add(evtsResult);
            
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
        
    public List<ResolvedPathType> findPath(String serviceType, EthernetBaseType ets, FindPathAlgorithmType algorithm, List<ResolvedPathType> po) throws Exception {
        log.error("findPath: The ETS service request is not supported!");
        throw new UnsupportedOperationException("ETS service request is not supported");
    }

    public List<ResolvedPathType> findPath(String serviceType, P2PServiceBaseType p2ps, FindPathAlgorithmType algorithm, List<ResolvedPathType> po) throws Exception {
        log.debug("findPath: P2PS path request");
        throw new UnsupportedOperationException("P2PS service request is not supported");
    }

    public List<ResolvedPathType> findPath(StpType src, StpType dst, FindPathAlgorithmType algorithm, List<ResolvedPathType> po) throws Exception {
        log.debug("findPath: ------------------------------------------------");
        assert false;

        PCEData pceData = new PCEData();

        TopoPathEndpoints pathEndpoints = new TopoPathEndpoints();
        pathEndpoints.setSrcLocal(src.getLocalId());
        pathEndpoints.setSrcNetwork(src.getNetworkId());

        pathEndpoints.setDstLocal(dst.getLocalId());
        pathEndpoints.setDstNetwork(dst.getNetworkId());
        pceData.getConstraints().add(pathEndpoints);
        pceData.setTopology(tp.getTopology());

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

        PCEData result = pce.apply(pceData);

        if (result == null) {
            throw new Exception("null result");
        } else if (result.getPath() == null) {

            throw new Exception("null path");
        }

        for (StpPair stpPair: result.getPath().getStpPairs() ) {
            String networkId = stpPair.getA().getNetwork().getNetworkId();

            ResolvedPathType pathObj = new ResolvedPathType();
            StpType aStpObj = new StpType();
            aStpObj.setLocalId(stpPair.getA().getLocalId());
            aStpObj.setNetworkId(networkId);
            StpType zStpObj = new StpType();
            zStpObj.setLocalId(stpPair.getZ().getLocalId());
            zStpObj.setNetworkId(networkId);

            //pathObj.setSourceStp = aStpObj;
            //pathObj.setDestStp(zStpObj);

            ServiceInfo si = sip.byNetworkId(networkId);

            pathObj.setNsa(si.getNsaId());
            pathObj.setCsProviderURL(si.getProviderUrl());


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
