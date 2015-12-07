/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.path.ResolvedPathType;
import net.es.nsi.pce.jaxb.path.TypeValueType;
import net.es.nsi.pce.pf.api.NsiError;
import net.es.nsi.pce.pf.api.PCEConstraints;
import net.es.nsi.pce.pf.api.Path;
import net.es.nsi.pce.pf.api.PathSegment;
import net.es.nsi.pce.pf.api.cons.AttrConstraint;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.BooleanAttrConstraint;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.NumAttrConstraint;
import net.es.nsi.pce.pf.api.cons.ObjectAttrConstraint;
import net.es.nsi.pce.pf.api.cons.StringAttrConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class Point2Point {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private final AttrConstraints constraints = new AttrConstraints();

    public Constraint addConstraint(P2PServiceBaseType service) {
        ObjectAttrConstraint constraint = new ObjectAttrConstraint();
        constraint.setAttrName(Point2PointTypes.P2PS);
        constraint.setValue(service);
        return constraint;
    }

    public Set<Constraint> addConstraints(P2PServiceBaseType service) {
        // Add requested capacity.
        NumAttrConstraint capacity = new NumAttrConstraint();
        capacity.setAttrName(Point2PointTypes.CAPACITY);
        capacity.setValue(service.getCapacity());
        constraints.add((AttrConstraint) capacity);

        // Add directionality.
        StringAttrConstraint directionality = new StringAttrConstraint();
        directionality.setAttrName(Point2PointTypes.DIRECTIONALITY);
        directionality.setValue(DirectionalityType.BIDIRECTIONAL.name());
        if (service.getDirectionality() != null) {
            directionality.setValue(service.getDirectionality().name());
        }
        constraints.add(directionality);

        // Add symmetric path if service is bidirectional.
        if (service.getDirectionality() != null && service.getDirectionality() == DirectionalityType.BIDIRECTIONAL) {
            BooleanAttrConstraint symmetricPath = new BooleanAttrConstraint();
            symmetricPath.setAttrName(Point2PointTypes.SYMMETRICPATH);
            symmetricPath.setValue(false);
            if (service.isSymmetricPath() != null) {
                symmetricPath.setValue(service.isSymmetricPath());
            }
            constraints.add(symmetricPath);
        }

        // Add the source STP.
        if (service.getSourceSTP() != null && !service.getSourceSTP().isEmpty()) {
            StringAttrConstraint srcStp = new StringAttrConstraint();
            srcStp.setAttrName(Point2PointTypes.SOURCESTP);
            srcStp.setValue(service.getSourceSTP());
            constraints.add(srcStp);
        }
        else {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), "null"));
        }

        // Add the destination STP.
        if (service.getDestSTP() != null && !service.getDestSTP().isEmpty()) {
            StringAttrConstraint dstStp = new StringAttrConstraint();
            dstStp.setAttrName(Point2PointTypes.DESTSTP);
            dstStp.setValue(service.getDestSTP());
            constraints.add(dstStp);
        }
        else {
            throw new IllegalArgumentException(NsiError.getFindPathErrorString(NsiError.MISSING_PARAMETER, Point2PointTypes.getDestStp().getNamespace(), Point2PointTypes.getDestStp().getType(), "null"));
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

    /**
     * Create a return message object for each path segment returned by PCE.
     *
     * @param path
     * @return
     */
    public List<ResolvedPathType> resolvePath(Path path) {
        List<ResolvedPathType> resolvedPath = new ArrayList<>();

        // For each pair of STP we need to build a resolved path.
        for (PathSegment segment : path.getPathSegments()) {
            // Convert the constraints.
            AttrConstraints pathConstraints = segment.getConstraints();
            StringAttrConstraint serviceType = pathConstraints.removeStringAttrConstraint(PCEConstraints.SERVICETYPE);
            ObjectAttrConstraint p2pObject = pathConstraints.removeObjectAttrConstraint(Point2PointTypes.P2PS);
            List<TypeValueType> attrConstraints = pathConstraints.removeStringAttrConstraints();

            // Results go here...
            ResolvedPathType pathObj = new ResolvedPathType();

            // Build our path finding results into an P2PS service.
            if (p2pObject != null) {
                P2PServiceBaseType p2psResult = p2pObject.getValue(P2PServiceBaseType.class);
                p2psResult.getParameter().addAll(attrConstraints);
                pathObj.getAny().add(factory.createP2Ps(p2psResult));
            }

            // Set the corresponding serviceType and add out EVTS results.
            if (serviceType != null) {
                pathObj.setServiceType(serviceType.getValue());
            }

            pathObj.setNsa(segment.getNsaId());
            pathObj.setCsProviderURL(segment.getCsProviderURL());

            resolvedPath.add(pathObj);
        }
        return resolvedPath;
    }
}
