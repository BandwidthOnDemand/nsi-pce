package net.es.nsi.pce.topology.model;

import com.google.common.base.Optional;
import static com.google.common.base.Strings.emptyToNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.es.nsi.pce.pf.StpTypeBundle;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.jaxb.topology.NetworkType;
import net.es.nsi.pce.jaxb.topology.NsaInterfaceType;
import net.es.nsi.pce.jaxb.topology.NsaType;
import net.es.nsi.pce.jaxb.topology.ReachabilityType;
import net.es.nsi.pce.jaxb.topology.ResourceRefType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.ServiceAdaptationType;
import net.es.nsi.pce.jaxb.topology.ServiceDomainType;
import net.es.nsi.pce.jaxb.topology.ServiceType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.jaxb.topology.VectorType;

/**
 *
 * @author hacksaw
 */
public class NsiTopology {
    // The NSI Topology model.
    private String localNsaId;
    private final List<String> localNetworks = new CopyOnWriteArrayList<>();
    private final ConcurrentSkipListMap<String, StpType> stps = new ConcurrentSkipListMap<>();
    private final ConcurrentHashMap<String, SdpType> sdps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ServiceAdaptationType> serviceAdaptations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ServiceDomainType> serviceDomains = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ServiceType> services = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NetworkType> networks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NsaType> nsas = new ConcurrentHashMap<>();

    private final ConcurrentSkipListMap<String, Map<String, StpType>> stpBundle = new ConcurrentSkipListMap<>();

    // The time of the most recent discovered item.
    private long lastDiscovered = 0L;

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
     *
     * @param stpList
     */
    public void putAllStp(Map<String, StpType> stpList) {
        stps.putAll(stpList);
    }

    /**
     * Add an STP object to the topology indexed by stpId.
     *
     * @param stpBundleId
     * @param stpBundle
     * @return NULL if this is the first object with this id, otherwise the STP
     * bundle map replaced.
     */
    public Map<String, StpType> addStpBundle(String stpBundleId, Map<String, StpType> stpBundle) {
        return this.stpBundle.put(stpBundleId, stpBundle);
    }

    public Map<String, StpType> getStpBundle(String stpBundleId) {
        return this.stpBundle.get(stpBundleId);
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
        return Collections.unmodifiableCollection(serviceAdaptations.values());
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
    public ServiceAdaptationType addServiceAdaptation(ServiceAdaptationType serviceAdaptation) {
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

    public void addAllNsa(Map<String, NsaType> nsas) {
        this.nsas.putAll(nsas);
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
        return Collections.unmodifiableCollection(stps.values());
    }

    public Map<String, StpType> getStpMap() {
        return Collections.unmodifiableSortedMap(stps);
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
        return Collections.unmodifiableSet(networks.keySet());
    }

    /**
     * Get the list of Network objects associated with this topology.
     *
     * @return Collection of Network objects from this topology.
     */
    public Collection<NetworkType> getNetworks() {
        return Collections.unmodifiableCollection(networks.values());
    }

    /**
     * Get a list of SDP in this Topology.
     *
     * @return List of SDP in a Collection.
     */
    public Collection<SdpType> getSdps() {
        return Collections.unmodifiableCollection(sdps.values());
    }

    /**
     * Get the list of SDP within this topology containing the specified STP
     * (if it exists).
     *
     * @param stpId The STP to locate in the list of SDP.
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
        return Collections.unmodifiableCollection(serviceDomains.values());
    }

    public Collection<ServiceType> getServices() {
        return Collections.unmodifiableCollection(services.values());
    }

    public Collection<NsaType> getNsas() {
        return Collections.unmodifiableCollection(nsas.values());
    }

    public Set<String> getNsaIds() {
        return Collections.unmodifiableSet(nsas.keySet());
    }

    /**
     * @return the lastDiscovered
     */
    public long getLastDiscovered() {
        return lastDiscovered;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastDiscovered(long lastDiscovered) {
        this.lastDiscovered = lastDiscovered;
    }

    /**
     * @return the localNsaId
     */
    public String getLocalNsaId() {
        return localNsaId;
    }

    /**
     * @param localNsaId the localNsaId to set
     */
    public void setLocalNsaId(String localNsaId) {
        this.localNsaId = localNsaId;
    }

    public Optional<String> getLocalProviderUrl() {
        return getProviderUrl(getLocalNsaId());
    }

    public Optional<String> getProviderUrl(String nsaId) {
        Optional<NsaType> nsa = Optional.fromNullable(getNsa(nsaId));
        if (!nsa.isPresent()) {
            return Optional.absent();
        }

        if (nsa.get().getInterface() == null) {
            return Optional.absent();
        }

        for (NsaInterfaceType anInteface : getNsa(nsaId).getInterface()) {
            if (NsiConstants.NSI_CS_PROVIDER_V2.equalsIgnoreCase(anInteface.getType())) {
                return Optional.fromNullable(emptyToNull(anInteface.getHref().trim()));
            }
        }
        return Optional.absent();
    }

    /**
     * @return the localNetworks
     */
    public List<String> getLocalNetworks() {
        return Collections.unmodifiableList(localNetworks);
    }

    /**
     * @param localNetworks the localNetworks to set
     */
    public void setLocalNetworks(List<String> localNetworks) {
        this.localNetworks.clear();
        this.localNetworks.addAll(localNetworks);
    }

    public Map<String, Map<String, Integer>> getReachabilityTable() {
        Map<String, Map<String, Integer>> table = new HashMap<>();
        for (NsaType nsa : getNsas()) {
            Map<String, Integer> vectors = new HashMap<>();
            for (ReachabilityType reachability : nsa.getReachability()) {
                for (VectorType vector : reachability.getVector()) {
                    vectors.put(vector.getId(), vector.getCost());
                }
            }
            table.put(nsa.getId(), vectors);
        }
        return table;
    }

    public NsiTopology getTopologyByNetworkId(String networkId) {
        NsiTopology tp = new NsiTopology();

        tp.setLocalNsaId(localNsaId);
        tp.setLocalNetworks(localNetworks);
        tp.setLastDiscovered(lastDiscovered);

        NetworkType network = getNetworkById(networkId);
        tp.addNetwork(network);

        // We need all the NSA for control plane path finding.
        tp.addAllNsa(nsas);

        for (ResourceRefType service : network.getService()) {
            tp.addService(getService(service.getId()));
        }

        for (ResourceRefType serviceDomain : network.getServiceDomain()) {
            tp.addServiceDomain(getServiceDomain(serviceDomain.getId()));
        }

        for (ResourceRefType serviceAdaptation : network.getServiceAdaptation()) {
            tp.addServiceAdaptation(getServiceAdaptation(serviceAdaptation.getId()));
        }

        for (ResourceRefType stp : network.getStp()) {
            tp.addStp(getStp(stp.getId()));
        }

        for (SdpType sdp : sdps.values()) {
            if (networkId.equalsIgnoreCase(sdp.getDemarcationA().getNetwork().getId()) &&
                    networkId.equalsIgnoreCase(sdp.getDemarcationZ().getNetwork().getId())) {
                tp.addSdp(sdp);
            }
        }

        return tp;
    }

    public Set<String> getExclusionSdp(StpTypeBundle stpBundle) {
        Set<String> exclusionSdp = new HashSet<>();
        Optional<Map<String, StpType>> bundle = Optional.fromNullable(this.getStpBundle(stpBundle.getSimpleStp().getId()));
        if (bundle.isPresent()) {
            for (StpType anStp : bundle.get().values()) {
                Optional<ResourceRefType> sdpRef = Optional.fromNullable(anStp.getSdp());
                if (sdpRef.isPresent()) {
                    exclusionSdp.add(sdpRef.get().getId());
                }
            }
        }

        return exclusionSdp;
    }
}
