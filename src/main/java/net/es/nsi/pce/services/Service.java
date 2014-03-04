package net.es.nsi.pce.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class is a temporary solution for the serviceType to service element
 * mapping that will be contained in the service description file.
 *
 * @author hacksaw
 */
public enum Service {
    P2PS("{http://schemas.ogf.org/nsi/2013/12/services/point2point}p2ps"),
    UNSUPPORTED("unsupported");

    // The QNAME associated with this enum instance.
    private final String qname;

    // Service mapping for A-GOLE service type.
    final static String EVTS_AGOLE = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";

    // Lists of service elements associated with the EVTS AGOLE service.
    private final static List<Service> EVTS_AGOLE_LIST = new ArrayList<Service>() {
        private static final long serialVersionUID = 1L;
        { add(P2PS); }
    };

    // Master list of supported services and mappings to elements.
    private final static HashMap<String, List<Service>> typeToServiceMap = new HashMap<String, List<Service>>() {
        private static final long serialVersionUID = 1L;
        {
            put(EVTS_AGOLE, EVTS_AGOLE_LIST);
        }
    };

    /**
     * Constructor to create enum members with string mapping to XML QNAME.
     *
     * @param text XML QNAME for service element.
     */
    private Service(final String qname) {
        this.qname = qname;
    }

    /**
     * Return string QNAME for the enum instead of a name value.
     *
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return qname;
    }

    /**
     * Get the QNAME associated with the enum element.
     *
     * @return QNAME of the enum element.
     */
    public String getQname() {
        return toString();
    }

    /**
     * Get the Service enum associated with the provided QNAME.
     *
     * @param qname XML namespace QNAME associated with the service element.
     * @return Service corresponding to QNAME.
     */
    public static Service getService(String qname) {
        Service[] values = Service.values();
        for (int i = 0; i < values.length; i++) {
            if (qname.equalsIgnoreCase(values[i].getQname())) {
                return values[i];
            }
        }

        return UNSUPPORTED;
    }

    /**
     * Get the Service associated with the string serviceType.
     *
     * @param type String serviceType associated with the supported service.
     *
     * @return List of Service elements associated with serviceType.
     */
    public static List<Service> getServiceByType(String type) {
        if (typeToServiceMap.containsKey(type)) {
            return typeToServiceMap.get(type);
        }
        else {
          return Collections.emptyList();
        }
    }
}
