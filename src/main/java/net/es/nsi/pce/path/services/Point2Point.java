/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.es.nsi.pce.path.jaxb.DirectionalityType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.path.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.path.jaxb.ResolvedPathType;
import net.es.nsi.pce.path.jaxb.TypeValueType;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.NumAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.AttrConstraint;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.Constraints;

/**
 *
 * @author hacksaw
 */
public class Point2Point {
  
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
    
    public List<ResolvedPathType> resolvePath(Path path) {
        List<ResolvedPathType> resolvedPath = new ArrayList<>();

        // For each pair of STP we need to build a resolved path.
        for (PathSegment segment : path.getPathSegments()) {
            // Convert the constraints.
            Constraints pathConstraints = segment.getConstraints();
            StringAttrConstraint serviceType = pathConstraints.removeStringAttrConstraint(PCEConstraints.SERVICETYPE);
            NumAttrConstraint capacity = pathConstraints.removeNumAttrConstraint(CAPACITY);
            StringAttrConstraint directionality = pathConstraints.removeStringAttrConstraint(DIRECTIONALITY);
            BooleanAttrConstraint symmetric = pathConstraints.removeBooleanAttrConstraint(SYMMETRICPATH);
            List<TypeValueType> attrConstraints = pathConstraints.removeStringAttrConstraints();
            
            StpPair stpPair = segment.getStpPair();

            // Build our path finding results into an P2PS service.
            P2PServiceBaseType p2psResult = factory.createP2PServiceBaseType();
            p2psResult.setSourceSTP(stpPair.getA().getId());
            p2psResult.setDestSTP(stpPair.getZ().getId());

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
            if (serviceType != null) {
                pathObj.setServiceType(serviceType.getValue());
            }
            pathObj.getAny().add(factory.createP2Ps(p2psResult));

            pathObj.setNsa(segment.getNsaId());
            pathObj.setCsProviderURL(segment.getCsProviderURL());

            resolvedPath.add(pathObj);
        }
        return resolvedPath;
    }
}
