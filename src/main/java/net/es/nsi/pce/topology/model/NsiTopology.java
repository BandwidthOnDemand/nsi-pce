package net.es.nsi.pce.topology.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.ServiceAdaptationType;
import net.es.nsi.pce.topology.jaxb.ServiceType;
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

    // The NSI Topology model.
    private ConcurrentHashMap<String, StpType> stps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, SdpType> sdps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceAdaptationType> serviceAdaptations = new ConcurrentHashMap<>();
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
     * 
     * @param topology
     * @return 
     */
    public NsiTopology add(NsiTopology topology) {
        stps.putAll(topology.getStpMap());
        sdps.putAll(topology.getSdpMap());
        serviceAdaptations.putAll(topology.getServiceAdaptationMap());
        serviceDomains.putAll(topology.getServiceDomainMap());
        services.putAll(topology.getServiceMap());
        networks.putAll(topology.getNetworkMap());
        nsas.putAll(topology.getNsaMap());
        return this;
    }
    
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
     * 
     * @param stpList 
     */
    public void addAllStp(Collection<StpType> stpList) {
        for (StpType stp : stpList) {
            stps.put(stp.getId().toLowerCase(), stp);
        }
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
     * 
     * @param sdpList 
     */
    public void addAllSdp(Collection<SdpType> sdpList) {
        for (SdpType sdp : sdpList) {
            sdps.put(sdp.getId().toLowerCase(), sdp);
        }
    }

    public Collection<ServiceAdaptationType> getServiceAdaptations() {
        return serviceAdaptations.values();
    }
        
    public ServiceAdaptationType getServiceAdaptation(String serviceAdaptationId) {
        return serviceAdaptations.get(serviceAdaptationId.toLowerCase());
    }
    
    public Map<String, ServiceAdaptationType> getServiceAdaptationMap() {
        return Collections.unmodifiableMap(serviceAdaptations);
    }
    
    /**
     * 
     * @param serviceAdaptationList 
     */
    public void addAllServiceAdaptations(Collection<ServiceAdaptationType> serviceAdaptationList) {
        for (ServiceAdaptationType serviceAdaptation : serviceAdaptationList) {
            serviceAdaptations.put(serviceAdaptation.getId().toLowerCase(), serviceAdaptation);
        }
    }

    /**
     * Add a ServiceAdaptationType object to the topology indexed by serviceAdaptationId.
     * 
     * @param serviceAdaptation The ServiceAdaptation object to store.
     * @return The ServiceAdaptation object stored.
     */
    public ServiceAdaptationType addService(ServiceAdaptationType serviceAdaptation) {
        return serviceAdaptations.put(serviceAdaptation.getId().toLowerCase(), serviceAdaptation);
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
     * 
     * @param sdList 
     */
    public void addAllServiceDomains(Collection<ServiceDomainType> sdList) {
        for (ServiceDomainType sd : sdList) {
            serviceDomains.put(sd.getId().toLowerCase(), sd);
        }
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
     * 
     * @param serviceList 
     */
    public void addAllServices(Collection<ServiceType> serviceList) {
        for (ServiceType service : serviceList) {
            services.put(service.getId().toLowerCase(), service);
        }
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
    
    public Map<String, SdpType> getSdpMap() {
        return Collections.unmodifiableMap(sdps);
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
    
    public Map<String, ServiceDomainType> getServiceDomainMap() {
        return Collections.unmodifiableMap(serviceDomains);
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
    
    public Map<String, ServiceType> getServiceMap() {
        return Collections.unmodifiableMap(services);
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
    
    public Map<String, NetworkType> getNetworkMap() {
        return Collections.unmodifiableMap(networks);
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
    
    public Map<String, NsaType> getNsaMap() {
        return Collections.unmodifiableMap(nsas);
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
    
    public Map<String, StpType> getStpMap() {
        return Collections.unmodifiableMap(stps);
    }   
    
    public Collection<StpType> getStpsByNetworkId(String networkId) {
        Collection<StpType> stpList = new ArrayList<>();
        
        NetworkType network = getNetworkById(networkId);
        if (network != null) {
            for (ResourceRefType stpRef : network.getStp()) {
                stpList.add(getStp(stpRef.getId()));
            }
        }
        
        return stpList;
    }
    
    /**
     * Clear the Topology.
     */
    public void clear() {
        stps.clear();
        sdps.clear();
        serviceAdaptations.clear();
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
            if (sdp.getDemarcationA().getStp().getId().equals(stpId) ||
                    sdp.getDemarcationZ().getStp().getId().equals(stpId)) {
                result.add(sdp);
            }
        }
        return result;
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
