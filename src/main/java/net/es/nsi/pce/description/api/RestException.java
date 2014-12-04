/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.description.api;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 *
 * @author hacksaw
 */
public class RestException {

    public static WebApplicationException internalServerErrorException(String resource, String parameter) {
        String error = RestError.serialize(RestError.INTERNAL_SERVER_ERROR, resource, parameter);
        Response ex = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException missingParameterException(String resource, String parameter) {
        String error = RestError.serialize(RestError.BAD_REQUEST, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException illegalArgumentException(String resource, String parameter) {
        String error = RestError.serialize(RestError.BAD_REQUEST, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException doesNotExistException(String resource, String parameter) {
        String error = RestError.serialize(RestError.NOT_FOUND, resource, parameter);
        Response ex = Response.status(Response.Status.NOT_FOUND).entity(error).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException resourceExistsException(String resource, String parameter) {
        String error = RestError.serialize(RestError.CONFLICT, resource, parameter);
        Response ex = Response.status(Response.Status.CONFLICT).entity(error).build();
        return new WebApplicationException(ex);
    }
}
