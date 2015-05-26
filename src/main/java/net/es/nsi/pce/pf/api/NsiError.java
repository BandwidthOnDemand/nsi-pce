package net.es.nsi.pce.pf.api;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import net.es.nsi.pce.path.jaxb.FindPathErrorType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
import net.es.nsi.pce.path.jaxb.VariableType;
import net.es.nsi.pce.schema.PathApiParser;

/**
 * Defines the error values for the PCE logging system.
 *
 * @author hacksaw
 */
public enum NsiError {
    // Message content errors.
    PAYLOAD_ERROR("00100", "PAYLOAD_ERROR", "Illegal message payload (%s)."),
    MISSING_PARAMETER("00101", "MISSING_PARAMETER", "Invalid or missing parameter (%s)."),
    UNSUPPORTED_PARAMETER("00102", "UNSUPPORTED_PARAMETER", "Parameter provided contains an unsupported value which MUST be processed (%s)."),
    NOT_IMPLEMENTED("00103", "NOT_IMPLEMENTED", "Parameter is for a feature that has not been implemented (%s)."),
    VERSION_NOT_SUPPORTED("00104", "VERSION_NOT_SUPPORTED", "The protocl version requested is not supported. (%s)."),

    // Connection errors.
    CONNECTION_ERROR("00200", "CONNECTION_ERROR", "A connection error has occured (%s)."),
    INVALID_TRANSITION("00201", "INVALID_TRANSITION", "Connection state machine is in invalid state for received message (%s)."),
    CONNECTION_EXISTS("00202", "CONNECTION_EXISTS", "Schedule already exists for connectionId (%s)."),
    CONNECTION_NONEXISTENT("00203", "CONNECTION_NONEXISTENT", "Schedule does not exists for connectionId (%s)."),
    CONNECTION_GONE("00204", "CONNECTION_GONE", "Requested connection does not exist (%s)."),
    CONNECTION_CREATE_ERROR("00205", "CONNECTION_CREATE_ERROR", "Failed to create connection (payload was ok, something went wrong) (%s)."),

    // Security errors.
    SECURITY_ERROR("00300", "SECURITY_ERROR", "A security error has occurred (%s)."),
    AUTHENTICATION_FAILURE("00301", "AUTHENTICATION_FAILURE", "Authentication failure (%s)."),
    UNAUTHORIZED("00302", "UNAUTHORIZED", "Authorization failure (%s)."),

    // Generic topology errors.
    TOPOLOGY_ERROR("00400", "TOPOLOGY_ERROR", "A topology error has occurred (%s)."),
    NO_PATH_FOUND("00403", "NO_PATH_FOUND", "Path computation failed (%s)."),
    UNKNOWN_NETWORK("00405", "UNKNOWN_NETWORK", "Unknown network for requested resource (%s)."),
    INVALID_DISCOVERY_INFORMATION("00406", "INVALID_DISCOVERY_INFORMATION", "Cannot map networkId to service interface (%s)."),
    NO_CONTROLPLANE_PATH_FOUND("00406", "NO_CONTROLPLANE_PATH_FOUND", "No control plane path for selected connection segements (%s)."),

    // Internal server errors.
    INTERNAL_ERROR("00500", "INTERNAL_ERROR", "An internal error has caused a message processing failure (%s)."),
    INTERNAL_NRM_ERROR("00501", "INTERNAL_NRM_ERROR", "An internal NRM error has caused a message processing failure (%s)."),

    // Resource availability errors.
    RESOURCE_UNAVAILABLE("00600", "RESOURCE_UNAVAILABLE", "An internal error has caused a message processing failure (%s)."),

    // Control plane error group.

    // Service specific errors.
    SERVICE_ERROR("00700", "SERVICE_ERROR", "A service specific error has occurred (%s)."),
    UNKNOWN_STP("00701", "UNKNOWN_STP", "Could not find STP in topology database (%s)."),
    STP_RESOLUTION_ERROR("00702", "STP_RESOLUTION_ERROR", "Could not resolve STP (%s)."),
    VLANID_INTERCANGE_NOT_SUPPORTED("00703", "VLANID_INTERCANGE_NOT_SUPPORTED", "VLAN interchange not supported for requested path (%s)."),
    STP_UNAVALABLE("00704", "STP_UNAVALABLE", "Specified STP already in use (%s)."),
    CAPACITY_UNAVAILABLE("00705", "CAPACITY_UNAVAILABLE", "Insufficient capacity available for reservation (%s)."),
    UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST("00706", "UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST", "A unidirectional STP was provided in a bidierctional request (%s)."),
    BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST("00707", "BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST", "A bidierctional STP was provided in a unidirectional request (%s)."),

    // Mark the end.
    END("99999", "END", "END");

    private String code;
    private String label;
    private String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<String, NsiError> codeToStatusMapping;

    private static ObjectFactory factory = new ObjectFactory();

    private NsiError(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (NsiError s : values()) {
            codeToStatusMapping.put(s.code, s);
        }
    }

    public static NsiError getStatus(String i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    public static FindPathErrorType getFindPathError(NsiError error, String namespace, String type, String value) {
        FindPathErrorType fp = factory.createFindPathErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setDescription(String.format(error.getDescription(), value));
        VariableType variable = new VariableType();
        variable.setNamespace(namespace);
        variable.setType(type);
        variable.setValue(value);
        fp.setVariable(variable);
        return fp;
    }

    public static FindPathErrorType getFindPathError(NsiError error, String description) {
        FindPathErrorType fp = factory.createFindPathErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setDescription(String.format(error.getDescription(), description));
        return fp;
    }

    public static FindPathErrorType getFindPathError(String xml) {
        try {
            @SuppressWarnings("unchecked")
            JAXBElement<FindPathErrorType> errorElement = (JAXBElement<FindPathErrorType>) PathApiParser.getInstance().stringToJaxb(xml);
            return errorElement.getValue();
        }
        catch (Exception ex) {
            return getFindPathError(NsiError.INTERNAL_ERROR, "Issue parsing FindPathErrorType error message");
        }
    }

    public static String getFindPathErrorString(NsiError error, String namespace, String type, String value) {
        FindPathErrorType fp = getFindPathError(error, namespace, type, value);
        JAXBElement<FindPathErrorType> errorElement = factory.createFindPathError(fp);
        String xml = PathApiParser.getInstance().jaxbToString(errorElement);
        return xml;
    }

    public static String getFindPathErrorString(NsiError error, String description) {
        FindPathErrorType fp = getFindPathError(error, description);
        JAXBElement<FindPathErrorType> errorElement = factory.createFindPathError(fp);
        String xml = PathApiParser.getInstance().jaxbToString(errorElement);
        return xml;
    }

    public static String getFindPathErrorString(FindPathErrorType fp) {
        JAXBElement<FindPathErrorType> errorElement = factory.createFindPathError(fp);
        String xml = PathApiParser.getInstance().jaxbToString(errorElement);
        return xml;
    }

    public String getCode() {
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
        sb.append("TopologyProviderStatus");
        sb.append("{ code=").append(code);
        sb.append(", label='").append(label).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
