/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.api;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.discovery.jaxb.ErrorType;
import net.es.nsi.pce.discovery.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.provider.InvalidVersionException;

/**
 *
 * @author hacksaw
 */
public class Exceptions {
    private static ObjectFactory factory = new ObjectFactory();

    public static WebApplicationException missingParameterException(String resource, String parameter) {
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
}
