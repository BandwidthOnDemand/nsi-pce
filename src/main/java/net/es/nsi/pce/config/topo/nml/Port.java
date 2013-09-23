/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo.nml;

import java.util.ArrayList;
import java.util.List;
import net.es.nsi.pce.topology.jaxb.LabelGroupType;
import net.es.nsi.pce.topology.jaxb.LabelType;

/**
 *
 * @author hacksaw
 */
public class Port {
    private String nsaId;
    private String topologyId;
    private String portId;
    private Directionality directionality;
    private Orientation orientation;
    private List<String> connectedTo = new ArrayList<String>();;
    private List<LabelType> labels = new ArrayList<LabelType>();
    private List<LabelGroupType> labelGroups = new ArrayList<LabelGroupType>();
    
    
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
    public String getPortId() {
        return portId;
    }

    /**
     * @param portId the portId to set
     */
    public void setPortId(String portId) {
        this.portId = portId;
    }

    /**
     * @return the directionality
     */
    public Directionality getDirectionality() {
        return directionality;
    }

    /**
     * @param directionality the directionality to set
     */
    public void setDirectionality(Directionality directionality) {
        this.directionality = directionality;
    }

    public boolean isBidirectional() {
        return (this.directionality == Directionality.bidirectional);
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
    public List<LabelType> getLabels() {
        return labels;
    }

    /**
     * @param labels the labels to set
     */
    public void setLabels(List<LabelType> labels) {
        this.labels = labels;
    }

    /**
     * @return the labelGroups
     */
    public List<LabelGroupType> getLabelGroups() {
        return labelGroups;
    }

    /**
     * @param labelGroups the labelGroups to set
     */
    public void setLabelGroups(List<LabelGroupType> labelGroups) {
        this.labelGroups = labelGroups;
    }

    /**
     * @return the labelGroups
     */
    public List<String> getConnectedTo() {
        return connectedTo;
    }
    
    /**
     * @param connectedTo the connectedTo to set
     */
    public void setConnectedTo(List<String> connectedTo) {
        this.connectedTo = connectedTo;
    }
}
