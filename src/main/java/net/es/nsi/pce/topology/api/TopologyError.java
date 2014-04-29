/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.api;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.TopologyErrorType;
import net.es.nsi.pce.topology.provider.TopologyParser;

/**
 *
 * @author hacksaw
 */
public enum TopologyError {
    // Message content errors.
    MISSING_PARAMETER(100, "MISSING_PARAMETER", "Missing parameter (%s)."),
    INVALID_PARAMETER(101, "INVALID_PARAMETER", "Invalid parameter (%s)."),
    UNSUPPORTED_PARAMETER(102, "UNSUPPORTED_PARAMETER", "Parameter provided contains an unsupported value which MUST be processed (%s)."),
    NOT_IMPLEMENTED(103, "NOT_IMPLEMENTED", "Parameter is for a feature that has not been implemented (%s)."),
    VERSION_NOT_SUPPORTED(104, "VERSION_NOT_SUPPORTED", "The service version requested is not supported. (%s)."),

    NOT_FOUND(106, "NOT_FOUND", "Requested resources was not found (%s)."),
    QUERY_LABEL_VALUE(107, "QUERY_LABEL_VALUE", "Query filter on labelValue (%s) must contain labelType."),
    PATH_PARAMETER(108, "PATH_PARAMETER", "Path parameter (%s) must be provided."),

    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "There was an internal server processing error (%s)."),

    // Mark the end.
    END(999, "END", "END");

    private int code;
    private String label;
    private String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, TopologyError> codeToStatusMapping;

    private static ObjectFactory factory = new ObjectFactory();

    private TopologyError(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (TopologyError s : values()) {
            codeToStatusMapping.put(s.code, s);
        }
    }

    public static TopologyError getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    public static TopologyErrorType getErrorType(TopologyError error, String resource, String info) {
        TopologyErrorType fp = factory.createTopologyErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setResource(resource);
        fp.setDescription(String.format(error.getDescription(), info));
        return fp;
    }

    public static Response badRequest(TopologyError error, String resource, String info) throws javax.ws.rs.WebApplicationException {
        TopologyErrorType fp = factory.createTopologyErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setResource(resource);
        fp.setDescription(String.format(error.getDescription(), info));
        JAXBElement<TopologyErrorType> element = factory.createTopologyError(fp);
        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<TopologyErrorType>>(element){}).build();
    }

    public static Response notFound(TopologyError error, String resource, String info) throws javax.ws.rs.WebApplicationException {
        TopologyErrorType fp = factory.createTopologyErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setResource(resource);
        fp.setDescription(String.format(error.getDescription(), info));
        JAXBElement<TopologyErrorType> element = factory.createTopologyError(fp);
        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<TopologyErrorType>>(element){}).build();
    }

    public static TopologyErrorType getErrorType(String xml) {
        TopologyErrorType error;
        try {
            @SuppressWarnings("unchecked")
            JAXBElement<TopologyErrorType> errorElement = (JAXBElement<TopologyErrorType>) TopologyParser.getInstance().stringToJaxb(xml);
            error = errorElement.getValue();
        }
        catch (JAXBException ex) {
            error = getErrorType(INTERNAL_SERVER_ERROR, "JAXB", xml);
        }
        return error;
    }

    public static String getErrorString(TopologyError error, String resource, String info) {
        TopologyErrorType fp = getErrorType(error, resource, info);
        JAXBElement<TopologyErrorType> errorElement = factory.createTopologyError(fp);
        String xml = TopologyParser.getInstance().jaxbToString(errorElement);
        return xml;
    }

    public static String getErrorString(TopologyErrorType error) {
        JAXBElement<TopologyErrorType> errorElement = factory.createTopologyError(error);
        String xml = TopologyParser.getInstance().jaxbToString(errorElement);
        return xml;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TopologyError ");
        sb.append("{ code=").append(code);
        sb.append(", label='").append(label).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
