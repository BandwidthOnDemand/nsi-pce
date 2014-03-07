/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.model.Orientation;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NmlPort {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private String id;
    private String nsaId;
    private String topologyId;
    private String name;
    private Orientation orientation;
    private Set<NmlLabelType> labels = new LinkedHashSet<>();
    private String connectedTo;
    private XMLGregorianCalendar discovered;
    private XMLGregorianCalendar version;
    private NmlPort inboundPort;
    private NmlPort outboundPort;

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }

    /**
     * @return the topologyId
     */
    public String getTopologyId() {
        return topologyId;
    }

    /**
     * @param topologyId the topologyId to set
     */
    public void setTopologyId(String topologyId) {
        this.topologyId = topologyId;
    }

    /**
     * @return the portId
     */
    public String getId() {
        return id;
    }

    /**
     * @param portId the portId to set
     */
    public void setId(String portId) {
        this.id = portId;
    }

    /**
     * @return the orientation
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set
     */
    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    /**
     * @return the labels
     */
    public Set<NmlLabelType> getLabels() {
        return labels;
    }

    /**
     * @param labels the labels to set
     */
    public void setLabels(Set<NmlLabelType> labels) {
        this.labels = labels;
    }

    /**
     * @return the connectedTo
     */
    public String getConnectedTo() {
        return connectedTo;
    }

    /**
     * @param connectedTo the connectedTo to set
     */
    public void setConnectedTo(String connectedTo) {
        this.connectedTo = connectedTo;
    }

    /**
     * @return the discovered
     */
    public XMLGregorianCalendar getDiscovered() {
        return discovered;
    }

    /**
     * @param discovered the discovered to set
     */
    public void setDiscovered(XMLGregorianCalendar discovered) {
        this.discovered = discovered;
    }
    
    /**
     * @param discovered the discovered to set
     */
    public void setDiscovered(long discovered) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(discovered);
        try {
            XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            this.discovered = newXMLGregorianCalendar;
        } catch (DatatypeConfigurationException ex) {
            log.error("setDiscovered: Failed to convert discovered time.");
        }
    }

    /**
     * @return the version
     */
    public XMLGregorianCalendar getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(XMLGregorianCalendar version) {
        this.version = version;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the inboundStpId
     */
    public NmlPort getInboundPort() {
        return inboundPort;
    }

    /**
     * @param inboundStpId the inboundStpId to set
     */
    public void setInboundPort(NmlPort inboundStpId) {
        this.inboundPort = inboundStpId;
    }

    /**
     * @return the outboundStpId
     */
    public NmlPort getOutboundPort() {
        return outboundPort;
    }

    /**
     * @param outboundStpId the outboundStpId to set
     */
    public void setOutboundPort(NmlPort outboundStpId) {
        this.outboundPort = outboundStpId;
    }
}
