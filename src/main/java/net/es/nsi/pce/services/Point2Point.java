/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import net.es.nsi.pce.api.jaxb.EthernetBaseType;
import net.es.nsi.pce.api.jaxb.EthernetVlanType;
import net.es.nsi.pce.api.jaxb.P2PServiceBaseType;
import net.es.nsi.pce.pf.api.cons.BurstSizeConstraint;
import net.es.nsi.pce.pf.api.cons.CapacityConstraint;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.DirectionalityConstraint;
import net.es.nsi.pce.pf.api.cons.MtuConstraint;
import net.es.nsi.pce.pf.api.cons.SymmetricPathConstraint;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;
import net.es.nsi.pce.topology.jaxb.LabelType;

/**
 *
 * @author hacksaw
 */
public class Point2Point {

    public static final String SYMMETRICPATH = "symmetricPath";

    public static final String BURSTSIZE = "burstsize";
    public static final String MTU = "mtu";
    public static final String VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
    
    public static Set<Constraint> getConstraints(P2PServiceBaseType service) {
        Set<Constraint> constraints = new HashSet<Constraint>();
        
        // Add requested capacity.
        CapacityConstraint capacity = new CapacityConstraint();
        capacity.setValue(service.getCapacity());
        constraints.add(capacity);
                
        // Add directionality.
        DirectionalityConstraint directionality = new DirectionalityConstraint();
        directionality.setValue(DirectionalityType.BIDIRECTIONAL);
        if (service.getDirectionality() != null) {
            directionality.setValue(service.getDirectionality());
        }
        constraints.add(directionality);
        
        // Add symmetric path.
        SymmetricPathConstraint symmetricPath = new SymmetricPathConstraint();
        symmetricPath.setSymmetricPath(service.isSymmetricPath());
        constraints.add(symmetricPath);

        // TODO: Still need to add these....
        service.getEro();
        
        if (service instanceof EthernetBaseType) {
            EthernetBaseType ets = (EthernetBaseType) service;
            
            // Add requested burstsize.
            if (ets.getBurstsize() != null) {
                BurstSizeConstraint burstsize = new BurstSizeConstraint();
                burstsize.setValue(ets.getBurstsize());
                constraints.add(burstsize);
            }

            // Add requested mtu.
            if (ets.getMtu() != null) {
                MtuConstraint mtu = new MtuConstraint();
                mtu.setValue(new Long(ets.getMtu()));
                constraints.add(mtu);
            }
        }
        
        // Add the source and destination STP to constraints.
        TopoPathEndpoints pe = new TopoPathEndpoints();
        
        pe.setSrcLocal(service.getSourceSTP().getLocalId());
        pe.setSrcNetwork(service.getSourceSTP().getNetworkId());
        pe.setDstLocal(service.getDestSTP().getLocalId());
        pe.setDstNetwork(service.getDestSTP().getNetworkId());
        
        if (service instanceof EthernetVlanType) {
            EthernetVlanType evts = (EthernetVlanType) service;
            LabelType srcLabel = new LabelType();
            srcLabel.setLabeltype(VLAN);
            srcLabel.setValue(Integer.toString(evts.getSourceVLAN()));
            pe.getSrcLabels().add(srcLabel);
            
            LabelType dstLabel = new LabelType();
            dstLabel.setLabeltype(VLAN);
            dstLabel.setValue(Integer.toString(evts.getDestVLAN()));
            pe.getDstLabels().add(dstLabel);
        }
        
        constraints.add(pe);
        
        return constraints;
    }
    
    /**
     * Find and return the VLAN label from within the Label list.
     * 
     * @param labels List of labels associated with an STP.
     * @return VLAN value if found, null otherwise.
     */
    public static Integer getVlanLabel(ArrayList<LabelType> labels) {
        for (LabelType label : labels) {
            if (label.getLabeltype().equalsIgnoreCase(VLAN)) {
                return Integer.valueOf(label.getValue());
            }
        }
        
        return null;
    }
}
