package net.es.nsi.pce.pf.api;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.path.jaxb.FindPathErrorType;
import net.es.nsi.pce.path.jaxb.ObjectFactory;
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
    VERSION_NOT_SUPPORTED("00104", "VERSION_NOT_SUPPORTED", "The service version requested is not supported. (%s)."),
    
    // Generic topology errors.
    TOPOLOGY_ERROR("00400", "TOPOLOGY_ERROR", "A topology error has occurred (%s)."),
    NO_PATH_FOUND("00403", "NO_PATH_FOUND", "Path computation failed (%s)."),
    UNKNOWN_NETWORK("00405", "UNKNOWN_NETWORK", "Unknown network for requested resource (%s)."),
    INVALID_DISCOVERY_INFORMATION("00406", "INVALID_DISCOVERY_INFORMATION", "Cannot map networkId to service interface (%s)."),
    
    // Service specific errors.
    SERVICE_ERROR("00700", "SERVICE_ERROR", "A service specific error has occurred (%s)."),
    UNKNOWN_STP("00701", "UNKNOWN_STP", "Could not find STP in topology database (%s)."),
    STP_RESOLUTION_ERROR("00702", "STP_RESOLUTION_ERROR", "Could not resolve STP to a managing NSA (%s)."),
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

    public static FindPathErrorType getFindPathError(NsiError error, String resource, String info) {
        FindPathErrorType fp = factory.createFindPathErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setResource(resource);
        fp.setDescription(String.format(error.getDescription(), info));
        return fp;
    }
    
    public static FindPathErrorType getFindPathError(String xml) throws JAXBException {
        @SuppressWarnings("unchecked")
        JAXBElement<FindPathErrorType> errorElement = (JAXBElement<FindPathErrorType>) PathApiParser.getInstance().stringToJaxb(xml);
        return errorElement.getValue();
    }
    
    public static String getFindPathErrorString(NsiError error, String resource, String info) {
        FindPathErrorType fp = getFindPathError(error, resource, info);
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
