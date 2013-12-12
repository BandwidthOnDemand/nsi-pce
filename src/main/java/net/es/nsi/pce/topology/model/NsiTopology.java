package net.es.nsi.pce.topology.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.config.topo.nml.EthernetPort;
import net.es.nsi.pce.config.topo.nml.Orientation;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.TypeValueType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.ServiceDomainType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NsiTopology {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final static String NML_LABEL_VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";

    // REST interface URL for each resource type.
    private static final String NSI_ROOT_STPS = "/topology/stps/";
    private static final String NSI_ROOT_SDPS = "/topology/sdps/";
    private static final String NSI_ROOT_SERVICEDOMAINS = "/topology/servicedomains/";
    private static final String NSI_ROOT_SERVICES = "/topology/services/";
    private static final String NSI_ROOT_NETWORKS = "/topology/networks/";
    private static final String NSI_ROOT_NSAS = "/topology/nsas/";
    
    // The NSI Topology model.
    private ConcurrentHashMap<String, StpType> stps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, SdpType> sdps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceDomainType> serviceDomains = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceType> services = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, NetworkType> networks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, NsaType> nsas = new ConcurrentHashMap<>();
    
    // The time of the most recent discovered item.
    private long lastModified = 0L;

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
        return stps.put(stp.getId().toLowerCase(), stp);
    }

    /**
     * Add an SDP object to the topology indexed by sdpId.
     * 
     * @param sdp The SDP object to store.
     * @return The SDP object stored.
     */
    public SdpType addSdp(SdpType sdp) {
        return sdps.put(sdp.getId().toLowerCase(), sdp);
    }
    
    /**
     * Add a ServiceDomain object to the topology indexed by serviceDomainId.
     * 
     * @param serviceDomain The ServiceDomain object to store.
     * @return The ServiceDomain object stored.
     */
    public ServiceDomainType addServiceDomain(ServiceDomainType serviceDomain) {
        return serviceDomains.put(serviceDomain.getId().toLowerCase(), serviceDomain);
    }

    /**
     * Add a ServiceType object to the topology indexed by serviceId.
     * 
     * @param service The Service object to store.
     * @return The Service object stored.
     */
    public ServiceType addService(ServiceType service) {
        return services.put(service.getId().toLowerCase(), service);
    }
    
    /**
     * Add a Network object to the topology indexed by networkId.
     * 
     * @param network The Network object to store.
     * @return The Network object stored.
     */
    public NetworkType addNetwork(NetworkType network) {
        return networks.put(network.getId().toLowerCase(), network);
    }
    
    /**
     * Add an NSA object to the topology indexed by nsaId.
     * 
     * @param nsa The NSA object to store.
     * @return The NSA object stored.
     */
    public NsaType addNsa(NsaType nsa) {
        return nsas.put(nsa.getId().toLowerCase(), nsa);
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
        return stps.get(stpId.toLowerCase());
    }

    /**
     * Get the SDP object corresponding to sdpId.
     * 
     * @param sdpId The SDP object to get.
     * @return The requested SDP object.
     */
    public SdpType getSdp(String sdpId) {
        return sdps.get(sdpId.toLowerCase());
    }    
    
    /**
     * Get the ServiceDomain object corresponding to serviceDomainId.
     * 
     * @param serviceDomainId The ServiceDomain object to get.
     * @return The requested ServiceDomain object.
     */
    public ServiceDomainType getServiceDomain(String serviceDomainId) {
        return serviceDomains.get(serviceDomainId.toLowerCase());
    }
    
    /**
     * Get the Service object corresponding to serviceId.
     * 
     * @param serviceId The Service object to get.
     * @return The requested Service object.
     */
    public ServiceType getService(String serviceId) {
        return services.get(serviceId.toLowerCase());
    }
    
    /**
     * Get the Network object corresponding to networkId.
     * 
     * @param networkId The Network object to get.
     * @return The requested Network object.
     */
    public NetworkType getNetwork(String networkId) {
        return networks.get(networkId.toLowerCase());
    }
    
    /**
     * Get the NSA object corresponding to nsaId.
     * 
     * @param nsaId The NSA object to get.
     * @return The requested NSA object.
     */
    public NsaType getNsa(String nsaId) {
        return nsas.get(nsaId.toLowerCase());
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
    
    public Collection<StpType> getStpsByNetworkId(String networkId) {
        Collection<StpType> stpList = new ArrayList<>();
        
        NetworkType network = getNetworkById(networkId);
        for (ResourceRefType stpRef : network.getStp()) {
            stpList.add(getStp(stpRef.getId()));
        }
        
        return stpList;
    }
    
    /**
     * Clear the Topology.
     */
    public void clear() {
        stps.clear();
        sdps.clear();
        serviceDomains.clear();
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
        NetworkType network = networks.get(networkId.toLowerCase());
        if (network == null) {
            return null;
        }
        return nsas.get(network.getNsa().getId().toLowerCase());
    }
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public NetworkType getNetworkById(String networkId) {
        return networks.get(networkId.toLowerCase());
    }
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public NetworkType getNetworkByName(String name) {
        for (NetworkType network : networks.values()) {
            if (name.equalsIgnoreCase(network.getName())) {
                return network;
            }
        }
        
        return null;
    }
    
    public Collection<NetworkType> getNetworksByNsaId(String nsaId) {
        ArrayList<NetworkType> results = new ArrayList<>();
        
        if (nsaId != null && !nsaId.isEmpty()) {
            for (NetworkType network : networks.values()) {             
                if (nsaId.equalsIgnoreCase(network.getNsa().getId())) {
                    results.add(network);
                }
            }
        }
        return results;
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
        stp.setId(port.getPortId() + "?vlan=" + vlanId.toString());
        stp.setName("unset in newStp");
        stp.setHref(NSI_ROOT_STPS + stp.getId());
        
        // Set the discovered time.
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(port.getDiscovered());
        try {
            XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            stp.setDiscovered(newXMLGregorianCalendar);
        } catch (DatatypeConfigurationException ex) {
            log.error("newStp: Failed to convert discovered time.");
        }

        // Set the NML version of the object.
        stp.setVersion(network.getVersion());

        // Determine the type of port.
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
        
        // Build the service resource type.
        ResourceRefType serviceType = new ResourceRefType();
        serviceType.setId("urn:ogf:network:netherlight.net:2012:service:EVTS.A-GOLE");
        serviceType.setHref("unset in newStp");
        serviceType.setType("http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE");
        stp.getService().add(serviceType);
        
        TypeValueType label = new TypeValueType();
        label.setType(NML_LABEL_VLAN);
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
            return  localId + "?" + postfix;
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
    
    public boolean labelEquals(TypeValueType a, TypeValueType b) {
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
    
    public int getVlanId(StpType stp) {
        return getVlanId(stp.getLabel());
    }
        
    public int getVlanId(TypeValueType label) {
        int vlanId = -1;
        
        if (NML_LABEL_VLAN.equalsIgnoreCase(label.getType()) &&
                label.getValue() != null && !label.getValue().isEmpty()) {
            vlanId = Integer.parseInt(label.getValue());
        }
        
        return vlanId;
    }
    
    public String getStringVlanId(TypeValueType label) {
        if (NML_LABEL_VLAN.equalsIgnoreCase(label.getType()) &&
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
        
        sdp.setHref(NSI_ROOT_SDPS + sdp.getId());

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
    
    public Collection<ServiceDomainType> getServiceDomains() {
        return serviceDomains.values();
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
    
    public NsaType newNsa(net.es.nsi.pce.nml.jaxb.NSAType nmlNsa) {
        NsaType nsiNsa = new NsaType();
        nsiNsa.setId(nmlNsa.getId());
        nsiNsa.setName(nmlNsa.getName());
        nsiNsa.setVersion(nmlNsa.getVersion());

        nsiNsa.setHref(NSI_ROOT_NSAS + nsiNsa.getId());
        
        if (nmlNsa.getLocation() != null) {
            nsiNsa.setLatitude(nmlNsa.getLocation().getLat());
            nsiNsa.setLongitude(nmlNsa.getLocation().getLong());
        }
        
        return nsiNsa;
    }
    
    public ResourceRefType newNsaRef(NsaType nsa) {
        ResourceRefType nsaRef = new ResourceRefType();
        nsaRef.setId(nsa.getId());
        nsaRef.setHref(nsa.getHref());
        return nsaRef;
    }
    
    public NetworkType newNetwork(net.es.nsi.pce.nml.jaxb.TopologyType nmlTopology, NsaType nsiNsa) {
        NetworkType nsiNetwork = new NetworkType();
        
        // Set the Id and naming information.
        nsiNetwork.setId(nmlTopology.getId());
        String name = nmlTopology.getName();
        if (name == null || name.isEmpty()) {
            name = nmlTopology.getId();
        }
        nsiNetwork.setName(name);

        // Create a direct reference to this Network object.
        nsiNetwork.setHref(NSI_ROOT_NETWORKS + nsiNetwork.getId());
        
        // Set the reference to the managing NSA.
        ResourceRefType nsiNsaRef = this.newNsaRef(nsiNsa);
        nsiNetwork.setNsa(nsiNsaRef);
        
        // Use the managing NSA values for discovered and version.
        nsiNetwork.setDiscovered(nsiNsa.getDiscovered());  
        nsiNetwork.setVersion(nsiNsa.getVersion());
        
        return nsiNetwork;
    }
    
    public ResourceRefType newNetworkRef(NetworkType network) {
        ResourceRefType nsaRef = new ResourceRefType();
        nsaRef.setId(network.getId());
        nsaRef.setHref(network.getHref());
        return nsaRef;
    }

    /**
     * @return the lastModified
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
