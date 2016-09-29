/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.path.api;

import com.google.common.base.Optional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.jaxb.path.FindPathErrorType;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.path.services.Point2PointTypes;
import net.es.nsi.pce.pf.api.NsiError;

/**
 *
 * @author hacksaw
 */
public class Exceptions {
    private static final ObjectFactory factory = new ObjectFactory();

    public static Optional<FindPathErrorType> getFindPathErrorType(WebApplicationException ex) {
        Optional<Response> response = Optional.fromNullable(ex.getResponse());
        if (response.isPresent()) {
            JAXBElement<FindPathErrorType> jaxb = (JAXBElement<FindPathErrorType>) response.get().getEntity();
            return Optional.fromNullable(jaxb.getValue());
        }
        return Optional.absent();
    }

    public static WebApplicationException stpUnknownNetwork(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.UNKNOWN_NETWORK, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException stpMissingLocalId(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.STP_MISSING_LOCAL_IDENTIFIER, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException stpUnknownLabelType(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.UNKNOWN_LABEL_TYPE, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException stpInvalidLabel(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.INVALID_LABEL_FORMAT, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException unsupportedParameter(String namespace, String type, String parameter) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.UNSUPPORTED_PARAMETER, namespace, type, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException missingParameter(String namespace, String type, String parameter) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.MISSING_PARAMETER, namespace, type, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

   public static WebApplicationException stpResolutionError(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.STP_RESOLUTION_ERROR, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

   public static WebApplicationException bidirectionalStpInUnidirectionalRequest(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.DIRECTIONALITY_MISMATCH, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

   public static WebApplicationException unidirectionalStpInBidirectionalRequest(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.DIRECTIONALITY_MISMATCH, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

   public static WebApplicationException noPathFound(String description) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.NO_PATH_FOUND, description);
        Response ex = Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

   public static WebApplicationException noControlPlanePathFound(String description) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.NO_CONTROLPLANE_PATH_FOUND, description);
        Response ex = Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException invalidEroError(String stpId) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.INVALID_ERO_FORMAT, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), stpId);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

   public static WebApplicationException noLocalNsaIdentifier(String description) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.NO_LOCAL_NSA_IDENTIFER, description);
        Response ex = Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }
   
   public static WebApplicationException internalServerError(String description) {
        FindPathErrorType error = NsiError.getFindPathError(NsiError.INTERNAL_ERROR, description);
        Response ex = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();
        return new WebApplicationException(ex);
    }

/* UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST
    public static WebApplicationException internalServerErrorException(String resource, String parameter) {
        String description = NsiError.getFindPathErrorString(NsiError.UNKNOWN_NETWORK, Point2PointTypes.getSourceStp().getNamespace(), Point2PointTypes.getSourceStp().getType(), srcStpId);
        FindPathErrorType error = NsiError.getFindPathError(NsiError.MISSING_PARAMETER, description);
        return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<FindPathErrorType>>(factory.createFindPathError(error)) {}).build();




        ErrorType error = DiscoveryError.getErrorType(NsiError.INTERNAL_ERROR, resource, parameter);
        Response ex = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException unauthorizedException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.UNAUTHORIZED, resource, parameter);
        Response ex = Response.status(Response.Status.UNAUTHORIZED).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException missingParameterException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException invalidParameterException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException invalidXmlException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException illegalArgumentException(DiscoveryError errorEnum, String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException doesNotExistException(DiscoveryError errorEnum, String resource, String id) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, id);
        Response ex = Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException resourceExistsException(DiscoveryError errorEnum, String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, parameter);
        Response ex = Response.status(Response.Status.CONFLICT).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static InvalidVersionException invalidVersionException(DiscoveryError errorEnum, String resource, XMLGregorianCalendar request, XMLGregorianCalendar actual) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, "request=" + request.toString() + ", actual=" + actual.toString());
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new InvalidVersionException(ex, request, actual);
    }
    */
}
