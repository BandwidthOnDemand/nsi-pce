package net.es.nsi.pce.topology.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.TypeValueType;
import net.es.nsi.pce.topology.provider.NmlPort;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NsiStpFactory {
    private final static Logger log = LoggerFactory.getLogger(NsiStpFactory.class);
    
    private final static String NML_LABEL_VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";

    // REST interface URL for each resource type.
    private static final String NSI_ROOT_STPS = "/topology/stps/";

    /**
     * 
     * @param port
     * @param label
     * @param nsiTopology
     * @return 
     */
    public static StpType createStpType(NmlPort port, NmlLabelType label, NsiTopology nsiTopology) {
        Logger log = LoggerFactory.getLogger(NsiStpFactory.class);
        
        // We want a new NSI STP resource.
        StpType stp = new StpType();

        // Build the STP Identifier using label if present.
        stp.setId(createStpId(port.getId(), label));

        stp.setName(port.getName());
        stp.setHref(NSI_ROOT_STPS + stp.getId());
        stp.setLocalId(port.getId());
        stp.setNetworkId(port.getTopologyId());
        stp.setDiscovered(port.getDiscovered());
        stp.setVersion(port.getVersion());

        // We need to handle Bidirectional STP differently.
        boolean bidirectional = false;
        if (port.getOrientation() == Orientation.inbound) {
            stp.setType(StpDirectionalityType.INBOUND);
        }
        else if (port.getOrientation() == Orientation.outbound) {
            stp.setType(StpDirectionalityType.OUTBOUND);
        }
        else {
            stp.setType(StpDirectionalityType.BIDIRECTIONAL);
            bidirectional = true;
        }

        // Build connectedTo id just like we did for this STP id.
        if (port.getConnectedTo() != null && !port.getConnectedTo().isEmpty()) {
            stp.setConnectedTo(createStpId(port.getConnectedTo(), label));
        }
        
        // For bidirectional connections we need to include references to the
        // member unidirectional STP which need to be populated in topology
        // before we process the bidirectional STP.
        if (bidirectional) {
            // Look up the inbound STP.
            NmlPort inboundPort = port.getInboundPort();
            String inboundStpId = createStpId(inboundPort.getId(), label);
            StpType inboundStp = nsiTopology.getStp(inboundStpId);
            if (inboundStp != null) {
                stp.setInboundStp(NsiStpFactory.createResourceRefType(inboundStp));
            }
            else {
                log.error("Bidirectional port " + stp.getId() + " has invalid inbound unidirectional member " + inboundStpId);
            }
            
            // Look up the outbound STP.
            NmlPort outboundPort = port.getOutboundPort();
            String outboundStpId = createStpId(outboundPort.getId(), label);
            StpType outboundStp = nsiTopology.getStp(outboundStpId);
            if (outboundStp != null) {
                stp.setOutboundStp(NsiStpFactory.createResourceRefType(outboundStp));
            }
            else {
                log.error("Bidirectional port " + stp.getId() + " has invalid outbound unidirectional member " + outboundStpId);
            }
        }

        // Add the original label in for tracking.
        if (label != null) {
            TypeValueType stpLabel = new TypeValueType();
            stpLabel.setType(label.getLabeltype());
            stpLabel.setValue(label.getValue());
            stp.setLabel(stpLabel);
        }

        // Finally, link this back into the containing Network resource.
        NetworkType network = nsiTopology.getNetworkById(stp.getNetworkId());
        stp.setNetwork(NsiNetworkFactory.createResourceRefType(network));
        
        return stp;
    }

    /**
     * 
     * @param port
     * @param nsiTopology
     * @return 
     */
    public static Collection<StpType> createStps(NmlPort port, NsiTopology nsiTopology) {
        ArrayList<StpType> stpList = new ArrayList<>();
        
        if (port.getLabels().isEmpty()) {
            StpType stp = createStpType(port, null, nsiTopology);
            stpList.add(stp);
        }
        else {
            for (NmlLabelType label : port.getLabels()) {
                // We want a new NSI STP resource.
                StpType stp = createStpType(port, label, nsiTopology);

                // Build the STP Identifier using label if present.
                log.debug("Creating STP " + stp.getId());
                stpList.add(stp);
            }
        }
        
        return stpList;
    }
    
    /**
     * <STP identifier> ::= "urn:ogf:network:" <networkId> “:” <localId> <label>
     * <label> ::= “?” <labelType> “=” <labelValue> | “?”<labelType> | “”
     * <labelType> ::= <string>
     * <labelValue> ::= <string>
     *
     * @param localId
     * @param label
     * @return 
     */
    public static String createStpId(String localId, NmlLabelType label) {
        // We need to build a label component for the STP Id.  NML uses label
        // types with a namespace and a # before the label type.  We want only
        // the label type string.
        StringBuilder identifier = new StringBuilder(localId);
        if (label != null) {
            String labelType = label.getLabeltype();
            if (labelType != null && !labelType.isEmpty()) {
                int pos = labelType.lastIndexOf("#");
                String type = labelType.substring(pos+1);
                if (type != null && !type.isEmpty()) {
                    identifier.append("?");
                    identifier.append(type);
                    
                    if (label.getValue() != null && !label.getValue().isEmpty()) {
                        identifier.append("=");
                        identifier.append(label.getValue());
                    }
                }
            }
        }
        
        return identifier.toString();
    }
    
    /**
     * 
     * @param stp
     * @return 
     */
    public static ResourceRefType createResourceRefType(StpType stp) {
        ResourceRefType stpRef = new ResourceRefType();
        stpRef.setId(stp.getId());
        stpRef.setHref(stp.getHref());

        return stpRef;
    }
    
    /**
     * 
     * @param a
     * @param b
     * @return 
     */
    public static boolean labelEquals(TypeValueType a, TypeValueType b) {
        if (a == null && b == null) {
            return true;
        }
        else if (a == null) {
            return false;
        }
        else if (b == null) {
            return false;
        }
        else if (!a.getType().equalsIgnoreCase(b.getType())) {
            return false;
        }
        
        if (a.getValue() == null && b.getValue() == null) {
            return true;
        }
        else if (b.getValue() == null) {
            return false;
        }
        else if (a.getValue() == null) {
            return false;
        }
        else if (!a.getValue().equalsIgnoreCase(b.getValue())) {
            return false;
        }        
        
        return true;
    }
    
    /**
     * 
     * @param a
     * @param b
     * @return 
     */
    public static boolean labelEquals(NmlLabelType a, NmlLabelType b) {
        if (a == null && b == null) {
            return true;
        }
        else if (a == null) {
            return false;
        }
        else if (b == null) {
            return false;
        }
        else if (!a.getLabeltype().equalsIgnoreCase(b.getLabeltype())) {
            return false;
        }
        
        if (a.getValue() == null && b.getValue() == null) {
            return true;
        }
        else if (b.getValue() == null) {
            return false;
        }
        else if (a.getValue() == null) {
            return false;
        }
        else if (!a.getValue().equalsIgnoreCase(b.getValue())) {
            return false;
        }        
        
        return true;
    }
    
    /**
     * 
     * @param stp
     * @return 
     */
    public static int getVlanId(StpType stp) {
        return getVlanId(stp.getLabel());
    }

    /**
     * 
     * @param label
     * @return 
     */
    public static int getVlanId(TypeValueType label) {
        int vlanId = -1;
        
        if (NML_LABEL_VLAN.equalsIgnoreCase(label.getType()) &&
                label.getValue() != null && !label.getValue().isEmpty()) {
            vlanId = Integer.parseInt(label.getValue());
        }
        
        return vlanId;
    }

    /**
     * 
     * @param label
     * @return 
     */
    public static String getStringVlanId(TypeValueType label) {
        if (NML_LABEL_VLAN.equalsIgnoreCase(label.getType()) &&
                label.getValue() != null && !label.getValue().isEmpty()) {
            return label.getValue();
        }
        
        return null;
    }
    
    public static List<StpType> ifModifiedSince(String ifModifiedSince, List<StpType> stpList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<StpType> iter = stpList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }
        
        return stpList;
    }
}
