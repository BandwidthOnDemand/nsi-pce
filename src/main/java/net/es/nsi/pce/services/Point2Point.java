/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.ObjectFactory;
import net.es.nsi.pce.api.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.api.jaxb.ResolvedPathType;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.NumAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.api.jaxb.TypeValueType;
import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.nsa.ServiceInfo;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.AttrConstraint;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.Constraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author hacksaw
 */
public class Point2Point {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String NAMESPACE = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps";

    // These are fixed element definitions within the P2P schema.
    public static final String CAPACITY = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/capacity";
    public static final String DIRECTIONALITY = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/directionality";
    public static final String SYMMETRICPATH = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/symmetricPath";
    public static final String SOURCESTP = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/sourceSTP";
    public static final String DESTSTP = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/destSTP";
    public static final String ERO = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/ero";
    
    // Parameters relating to the EVTS service.
    public static final String BURSTSIZE = "http://schemas.ogf.org/nml/2012/10/ethernet#burstsize";
    public static final String MTU = "http://schemas.ogf.org/nml/2012/10/ethernet#mtu";
    public static final String VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";

    private ApplicationContext context = null;
    private ServiceInfoProvider sip = null;
    private ObjectFactory factory = new ObjectFactory();
    private Constraints constraints = new Constraints();

    public Set<Constraint> addConstraints(P2PServiceBaseType service) {
        // Add requested capacity.
        NumAttrConstraint capacity = new NumAttrConstraint();
        capacity.setAttrName(CAPACITY);
        capacity.setValue(service.getCapacity());
        constraints.add((AttrConstraint) capacity);
         
        // Add directionality.
        StringAttrConstraint directionality = new StringAttrConstraint();
        directionality.setAttrName(DIRECTIONALITY);
        directionality.setValue(DirectionalityType.BIDIRECTIONAL.name());
        if (service.getDirectionality() != null) {
            directionality.setValue(service.getDirectionality().name());
        }
        constraints.add(directionality);
      
        // Add symmetric path if service is bidirectional.
        if (service.getDirectionality() != null && service.getDirectionality() == DirectionalityType.BIDIRECTIONAL) {
            BooleanAttrConstraint symmetricPath = new BooleanAttrConstraint();
            symmetricPath.setAttrName(SYMMETRICPATH);
            symmetricPath.setValue(false);
            if (service.isSymmetricPath() != null) {
                symmetricPath.setValue(service.isSymmetricPath());
            }
            constraints.add(symmetricPath);
        }
          
        // Add the source STP.
        if (service.getSourceSTP() != null && !service.getSourceSTP().isEmpty()) {
            StringAttrConstraint srcStp = new StringAttrConstraint();
            srcStp.setAttrName(SOURCESTP);
            srcStp.setValue(service.getSourceSTP());
            constraints.add(srcStp);
        }
        else {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, "p2ps", SOURCESTP));
        }
          
        // Add the destination STP.
        if (service.getDestSTP() != null && !service.getDestSTP().isEmpty()) {
            StringAttrConstraint dstStp = new StringAttrConstraint();
            dstStp.setAttrName(DESTSTP);
            dstStp.setValue(service.getDestSTP());
            constraints.add(dstStp);
        }
        else {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, "p2ps", DESTSTP));
        }
  
        // TODO: Still need to add these....
        //service.getEro();
        
        // Now add all the generic parameters as string attributes.
        for (TypeValueType parameter : service.getParameter()) {
            StringAttrConstraint generic = new StringAttrConstraint();
            generic.setAttrName(parameter.getType());
            generic.setValue(parameter.getValue());
            constraints.add(generic);
        }
     
        return constraints.get();
    }
    
    public List<ResolvedPathType> resolvePath(String serviceType, Path path) {
        List<ResolvedPathType> resolvedPath = new ArrayList<>();

        // TODO: These are copied directly from the request criteria for
        // but will need to be specific per segment in the future.
        NumAttrConstraint capacity = constraints.removeNumAttrConstraint(CAPACITY);
        StringAttrConstraint directionality = constraints.removeStringAttrConstraint(DIRECTIONALITY);
        BooleanAttrConstraint symmetric = constraints.removeBooleanAttrConstraint(SYMMETRICPATH);
        constraints.removeStringAttrConstraint(SOURCESTP);
        constraints.removeStringAttrConstraint(DESTSTP);
        List<TypeValueType> attrConstraints = constraints.removeStringAttrConstraints();
        
        // For each pair of STP we need to build a resolved path.
        for (StpPair stpPair: path.getStpPairs() ) {
            // Build our path finding results into an P2PS service.
            P2PServiceBaseType p2psResult = factory.createP2PServiceBaseType();
            p2psResult.setSourceSTP(stpPair.getA().getId());
            p2psResult.setDestSTP(stpPair.getZ().getId());
            
            // Get the managing network of this STP pair.
            String networkId = stpPair.getA().getNetworkId();

            if (capacity != null) {
                p2psResult.setCapacity(capacity.getValue());
            }
            
            if (directionality != null) {
                p2psResult.setDirectionality(DirectionalityType.valueOf(directionality.getValue()));
            }

            if (symmetric != null) {
                p2psResult.setSymmetricPath(symmetric.getValue());
            }
            
            p2psResult.getParameter().addAll(attrConstraints);
            
            // Set the corresponding serviceType and add out EVTS results.
            ResolvedPathType pathObj = new ResolvedPathType();
            pathObj.setServiceType(serviceType);
            pathObj.getAny().add(factory.createP2Ps(p2psResult));
            
            /* Look up the managing NSA for this path segment.  TODO: This
             * should use control plane topology to determine the next NSA to
             * pass the request just in case we cannot talk to the managing NSA
             * directly.
             */
            SpringContext sc  = SpringContext.getInstance();
            context = sc.getContext();
            sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
            ServiceInfo si = sip.byNetworkId(networkId);
            pathObj.setNsa(si.getNsaId());
            pathObj.setCsProviderURL(si.getProviderUrl());
            resolvedPath.add(pathObj);
        }

        return resolvedPath;
    }
}
