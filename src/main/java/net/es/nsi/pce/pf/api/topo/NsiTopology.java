/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.api.topo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.LabelType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.TransferServiceType;

/**
 *
 * @author hacksaw
 */
public class NsiTopology {
    private final static String NML_LABEL_VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
    
    private ConcurrentHashMap<String, StpType> stps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, SdpType> sdps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, TransferServiceType> transferServices = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceType> services = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, NetworkType> networks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, NsaType> nsas = new ConcurrentHashMap<>();

    /*************************************************************************
     * The base type adders.
     *************************************************************************/
    
    /**
     * Add an STP object to the topology indexed by stpId.
     * 
     * @param stp The STP object to store.
     * @return The STP object stored.
     */
    public StpType addStp(StpType stp) {
        return stps.put(stp.getId(), stp);
    }

    /**
     * Add an SDP object to the topology indexed by sdpId.
     * 
     * @param sdp The SDP object to store.
     * @return The SDP object stored.
     */
    public SdpType addSdp(SdpType sdp) {
        return sdps.put(sdp.getId(), sdp);
    }
    
    /**
     * Add a TransferService object to the topology indexed by transferServiceId.
     * 
     * @param transferService The TransferService object to store.
     * @return The TransferService object stored.
     */
    public TransferServiceType addTransferService(TransferServiceType transferService) {
        return transferServices.put(transferService.getId(), transferService);
    }

    /**
     * Add a ServiceType object to the topology indexed by serviceId.
     * 
     * @param service The Service object to store.
     * @return The Service object stored.
     */
    public ServiceType addService(ServiceType service) {
        return services.put(service.getId(), service);
    }
    
    /**
     * Add a Network object to the topology indexed by networkId.
     * 
     * @param network The Network object to store.
     * @return The Network object stored.
     */
    public NetworkType addNetwork(NetworkType network) {
        return networks.put(network.getId(), network);
    }
    
    /**
     * Add an NSA object to the topology indexed by nsaId.
     * 
     * @param nsa The NSA object to store.
     * @return The NSA object stored.
     */
    public NsaType addNsa(NsaType nsa) {
        return nsas.put(nsa.getId(), nsa);
    }
    
    /*************************************************************************
     * The base type getters.
     *************************************************************************/
    
    /**
     * Get the STP object corresponding to stpId.
     * 
     * @param stpId The STP object to get.
     * @return The requested STP object.
     */
    public StpType getStp(String stpId) {
        return stps.get(stpId);
    }

    /**
     * Get the SDP object corresponding to sdpId.
     * 
     * @param sdpId The SDP object to get.
     * @return The requested SDP object.
     */
    public SdpType getSdp(String sdpId) {
        return sdps.get(sdpId);
    }    
    
    /**
     * Get the TransferService object corresponding to transferServiceId.
     * 
     * @param transferServiceId The TransferService object to get.
     * @return The requested TransferService object.
     */
    public TransferServiceType getTransferService(String transferServiceId) {
        return transferServices.get(transferServiceId);
    }
    
    /**
     * Get the Service object corresponding to serviceId.
     * 
     * @param serviceId The Service object to get.
     * @return The requested Service object.
     */
    public ServiceType getService(String serviceId) {
        return services.get(serviceId);
    }
    
    /**
     * Get the Network object corresponding to networkId.
     * 
     * @param networkId The Network object to get.
     * @return The requested Network object.
     */
    public NetworkType getNetwork(String networkId) {
        return networks.get(networkId);
    }
    
    /**
     * Get the NSA object corresponding to nsaId.
     * 
     * @param nsaId The NSA object to get.
     * @return The requested NSA object.
     */
    public NsaType getNsa(String nsaId) {
        return nsas.get(nsaId);
    }
    
    /*************************************************************************
     * Convenience methods.
     *************************************************************************/    

    /**
     * Get the collection of STPs currently in the NSI topology.
     * 
     * @return The collection of STPs currently in the NSI topology.
     */
    public Collection<StpType> getStps() {
        return stps.values();
    }
    
    /**
     * Clear the Topology.
     */
    public void clear() {
        stps.clear();
        sdps.clear();
        transferServices.clear();
        services.clear();
        networks.clear();
        nsas.clear();
    }
    
    /************************************************************************/
    
    /**
     * 
     * @param network
     * @return NSA
     */
    public NsaType getNsaByNetwork(NetworkType network) {
        return nsas.get(network.getNsa().getId());
    }
    
    /**
     * 
     * @param networkId
     * @return 
     */
    public NsaType getNsaByNetworkId(String networkId) {
        NetworkType network = networks.get(networkId);
        if (network == null) {
            return null;
        }
        return nsas.get(network.getNsa().getId());
    }
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public NetworkType getNetworkById(String networkId) {
        return networks.get(networkId);
    }
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public NetworkType getNetworkByName(String name) {
        for (NetworkType network : networks.values()) {
            if (name.contentEquals(network.getName())) {
                return network;
            }
        }
        
        return null;
    }
    
    /**
     * Get a set of networkIds associated with this topology.
     * 
     * @return Set of networkIds.
     */
    public Set<String> getNetworkIds() {
        return networks.keySet();
    }

    /**
     * Get the list of Network objects associated with this topology.
     * 
     * @return Collection of Network objects from this topology.
     */
    public Collection<NetworkType> getNetworks() {
        return networks.values();
    }

    /**
     * Get a list of SDP in this Topology.
     * 
     * @return List of SDP in a Collection.
     */
    public Collection<SdpType> getSdps() {
        return sdps.values();
    }
    
    /**
     * Get the list of SDP within this topology containing the specified STP
     * (if it exists).
     * 
     * @param stp The STP to locate in the list of SDP.
     * @return The matching list of SDP.
     */
    public Set<SdpType> getSdpMember(String stpId) {
        HashSet<SdpType> result = new HashSet<>();
        for (SdpType sdp : sdps.values()) {
            if (sdp.getStpA().getId().equals(stpId) || sdp.getStpZ().getId().equals(stpId)) {
                result.add(sdp);
            }
        }
        return result;
    }
    
    public StpType newStp(NetworkType network, EthernetPort port, Integer vlanId) {
        // We want a new NSI STP.
        StpType stp = new StpType();
        
        // Set the STP attributes.
        stp.setId(port.getPortId() + ":vlan=" + vlanId.toString());
        stp.setName("unset in newStp");
        stp.setHref("unset in newStp");
        
        if (port.isBidirectional()) {
            stp.setType(StpDirectionalityType.BIDIRECTIONAL_PORT);
        }
        else if (port.getOrientation() == Orientation.inbound) {
            stp.setType(StpDirectionalityType.INBOUND_PORT);
        }
        else if (port.getOrientation() == Orientation.outbound) {
            stp.setType(StpDirectionalityType.OUTBOUND_PORT);
        }
        
        // Set the STP elements.
        stp.setLocalId(port.getPortId());
        stp.setNetworkId(network.getId());
        
        ResourceRefType serviceType = new ResourceRefType();
        serviceType.setId("unset in newStp");
        serviceType.setHref("unset in newStp");
        serviceType.setType("unset in newStp");
        stp.getServiceType().add(serviceType);
        
        LabelType label = new LabelType();
        label.setLabeltype(NML_LABEL_VLAN);
        label.setValue(vlanId.toString());
        stp.setLabel(label);

        return stp;
    }
    
    public String newStpId(String neworkId, String localId, net.es.nsi.pce.nml.jaxb.LabelType label) {
        // We need to build a label component for the STP Id.  NML uses label
        // types with a namespace and a # before the label type.  We want only
        // the label type string.
        String postfix = null;
        if (label != null) {
            String type = label.getLabeltype();
            String value = label.getValue();
            int pos = type.lastIndexOf("#");
            postfix = type.substring(pos+1) + "=" + value;
            
        }
        
        // Build the internal format for STP identifier.
        if (localId != null && postfix != null) {
            return  localId + ":" + postfix;
        }
        
        return localId;
    }
    
    public ResourceRefType newStpRef(StpType stp) {
        ResourceRefType stpRef = new ResourceRefType();
        stpRef.setId(stp.getId());
        stpRef.setHref(stp.getHref());
        stpRef.setType("unset in newStpRef");

        return stpRef;
    }
    
    public boolean labelEquals(LabelType a, LabelType b) {
        if (a == null && b == null) {
            return true;
        }
        else if (a == null) {
            return false;
        }
        else if (b == null) {
            return false;
        }
        else if (!a.getLabeltype().contentEquals(b.getLabeltype())) {
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
        else if (!a.getValue().contentEquals(b.getValue())) {
            return false;
        }        
        
        return true;
    }
    
    public int getVlanId(StpType stp) {
        return getVlanId(stp.getLabel());
    }
        
    public int getVlanId(LabelType label) {
        int vlanId = -1;
        
        if (NML_LABEL_VLAN.equalsIgnoreCase(label.getLabeltype()) &&
                label.getValue() != null && !label.getValue().isEmpty()) {
            vlanId = Integer.parseInt(label.getValue());
        }
        
        return vlanId;
    }
    
    public String getStringVlanId(LabelType label) {
        if (NML_LABEL_VLAN.equalsIgnoreCase(label.getLabeltype()) &&
                label.getValue() != null && !label.getValue().isEmpty()) {
            return label.getValue();
        }
        
        return null;
    }
    
    public SdpType newSdp(StpType stpA, StpType stpZ) {
        // We want a new NSI STP.
        SdpType sdp = new SdpType();

        // Set the STP attributes.
        sdp.setId(stpA.getId() + "::" + stpZ.getId());
        sdp.setName("unset in newSdp");
        sdp.setHref("unset in newSdp");

        // Set the STP references.
        sdp.setStpA(this.newStpRef(stpA));
        sdp.setStpZ(this.newStpRef(stpZ));
        
        // Determine the type of SDP.
        if (stpA.getType() == StpDirectionalityType.BIDIRECTIONAL_PORT &&
               stpZ.getType() == StpDirectionalityType.BIDIRECTIONAL_PORT) {
            sdp.setType(SdpDirectionalityType.BIDIRECTIONAL);
        }
        else if (stpA.getType() == StpDirectionalityType.INBOUND_PORT &&
               stpZ.getType() == StpDirectionalityType.OUTBOUND_PORT ||
               stpA.getType() == StpDirectionalityType.OUTBOUND_PORT &&
               stpZ.getType() == StpDirectionalityType.INBOUND_PORT) {
            sdp.setType(SdpDirectionalityType.UNIDIRECTIONAL);
        }
        else {
            sdp.setType(SdpDirectionalityType.UNDEFINED);
        }
            
        return sdp;
    }
    
    public ResourceRefType newServiceRef(ServiceType service) {
        ResourceRefType serviceRef = new ResourceRefType();
        serviceRef.setId(service.getId());
        serviceRef.setHref(service.getHref());
        serviceRef.setType("unset in newServiceRef");

        return serviceRef;
    }
    
    public Collection<TransferServiceType> getTransferServices() {
        return transferServices.values();
    }
    
    public Collection<ServiceType> getServices() {
        return services.values();
    }
    
    public Collection<NsaType> getNsas() {
        return nsas.values();
    }
    
    public Set<String> getNsaIds() {
        return nsas.keySet();
    }
}
