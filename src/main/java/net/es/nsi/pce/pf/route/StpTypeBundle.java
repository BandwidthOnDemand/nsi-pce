package net.es.nsi.pce.pf.route;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.topology.SdpType;
import net.es.nsi.pce.jaxb.topology.StpDirectionalityType;
import net.es.nsi.pce.jaxb.topology.StpType;
import net.es.nsi.pce.jaxb.topology.TypeValueType;
import net.es.nsi.pce.path.api.Exceptions;
import net.es.nsi.pce.pf.simple.SimpleLabel;
import net.es.nsi.pce.pf.simple.SimpleStp;
import net.es.nsi.pce.topology.model.NsiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will decompose an under specified STP into a list of fully
 * specified STP.
 *
 * For example, the following STP:
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7000-7005
 *
 * Would result in the following six individual STP:
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7000
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7001
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7002
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7003
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7004
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1?vlan=7005
 *
 * Similarly, the following STP would result in a set of fully qualified STP,
 * one for each VLAN specified against the STP:
 *    urn:ogf:network:canarie.ca:2017:topology:ANA1
 *
 * @author hacksaw
 */
public class StpTypeBundle {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Map<String, StpType> bundle = new HashMap<>();
  private final SimpleStp stp;

  /**
   * Constructor for class - For the provided STP (possibly under specified)
   * return a list of fully qualified STP matching
   *
   * @param topology
   * @param stp
   * @param directionality
   */
  public StpTypeBundle(NsiTopology topology, SimpleStp stp, DirectionalityType directionality) {
    this.stp = stp;
    // If this is an underspecified STP with no labels...
    if (stp.isRoot()) {
      // We need to add all possible label values associated with this STP.
      StpTypeBundle stpTypeBundle = topology.getStpTypeBundle(stp.getId());
      if (stpTypeBundle == null) {
        log.info("StpTypeBundle: could not find stpId=" + stp.getId());
        return;
      }

      for (StpType s : stpTypeBundle.values()) {
        if (validateDirectionality(s, directionality)) {
          bundle.put(s.getId(), s);
        }
      }
    } else {
      for (String stpId : stp.getMemberStpId()) {
        // We need special handling for underspecified STP.
        Optional<StpType> stpType = Optional.fromNullable(topology.getStp(stpId));
        if (stpType.isPresent() && validateDirectionality(stpType.get(), directionality)) {
          bundle.put(stpId, stpType.get());
        }
      }
    }
  }

  public StpTypeBundle(SimpleStp stp, Map<String, StpType> stpBundle) {
    this.stp = stp;
    bundle.putAll(stpBundle);
  }

  public StpTypeBundle() {
    stp = new SimpleStp();
  }

  public void addStpType(StpType stpType) {
    bundle.put(stpType.getId(), stpType);
    stp.addStpId(stpType.getId());
  }

  public SimpleStp getSimpleStp() {
    return stp;
  }

  public Map<String, StpType> getStpBundle() {
    return Collections.unmodifiableMap(bundle);
  }

  public Collection<StpType> values() {
    return bundle.values();
  }

  public Set<String> keySet() {
    return bundle.keySet();
  }

  private boolean validateDirectionality(StpType stp, DirectionalityType directionality) throws IllegalArgumentException {
    // Verify the specified STP are of the correct type for the request.
    if (directionality == DirectionalityType.UNIDIRECTIONAL) {
      if (stp.getType() != StpDirectionalityType.INBOUND
              && stp.getType() != StpDirectionalityType.OUTBOUND) {
        return false;
      }
    } else {
      if (stp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
        return false;
      }
    }

    return true;
  }

  /**
   * Restrict the provided STP bundle to contain only the STP corresponding to
   * the remote end of the associated SDP.
   *
   * @param topology
   * @param stp
   * @param directionality
   * @return
   */
  public StpTypeBundle getPeerRestrictedBundle(NsiTopology topology, Optional<StpType> stp,
          DirectionalityType directionality) {
    if (stp.isPresent()) {
      Optional<StpTypeBundle> peer = getPeerBundle(topology, stp.get(), directionality);
      if (peer.isPresent()) {
        return peer.get();
      } else {
        throw Exceptions.invalidEroError(stp.get().getId());
      }
    }

    return this;
  }

  /**
   * Return an StpTypeBundle containing only STP that have associated SDP.
   * This StpTypeBundle will remain unaltered.
   *
   * @return
   */
  public StpTypeBundle getPeerReducedBundle() {
    // We will
    SimpleStp resultp = new SimpleStp();
    resultp.setNetworkId(stp.getNetworkId());
    resultp.setLocalId(stp.getLocalId());

    // Brute force - iterate through each STP to see if it has an associated SDP.
    Map<String, StpType> results = new HashMap<>();
    bundle.values().stream()
            .filter((s) -> (s.getSdp() != null && !Strings.isNullOrEmpty(s.getSdp().getId())))
            .forEachOrdered((s) -> {
              TypeValueType tvt = s.getLabel();
              SimpleLabel label = new SimpleLabel(tvt.getType(), tvt.getValue());
              resultp.addLabel(label);
              results.put(s.getId(), s);
            }
    );

    return new StpTypeBundle(resultp, results);
  }

  /**
   * Get the peer bundle for the remote end of the SDP associated with the provided STP.
   *
   * @param topology
   * @param stp
   * @param directionality
   * @return
   */
  public static Optional<StpTypeBundle> getPeerBundle(NsiTopology topology, StpType stp, DirectionalityType directionality) {
    Optional<StpTypeBundle> resultBundle = Optional.absent();
    Optional<SdpType> sdp = Optional.fromNullable(topology.getSdp(stp.getSdp().getId()));
    if (sdp.isPresent()) {
      SimpleStp peer;
      if (sdp.get().getDemarcationA().getStp().getId().equalsIgnoreCase(stp.getId())) {
        peer = new SimpleStp(topology.getStp(sdp.get().getDemarcationZ().getStp().getId()).getId());
      } else {
        peer = new SimpleStp(topology.getStp(sdp.get().getDemarcationA().getStp().getId()).getId());
      }
      resultBundle = Optional.of(new StpTypeBundle(topology, peer, directionality));
    }

    return resultBundle;
  }

  public int size() {
    return bundle.size();
  }

  public boolean isEmpty() {
    return bundle.isEmpty();
  }

  public boolean contains(Object o) {
    return bundle.containsValue((StpType) o);
  }

  public Iterator iterator() {
    return bundle.values().iterator();
  }

  public Object[] toArray() {
    return bundle.values().toArray();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if ((object == null) || (object.getClass() != this.getClass())) {
      return false;
    }

    StpTypeBundle that = (StpTypeBundle) object;
    if (!this.stp.equals(that.getSimpleStp())) {
      return false;
    }

    return this.bundle.equals(that.getStpBundle());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((stp == null) ? 0 : stp.hashCode());
    result = prime * result
            + ((bundle == null) ? 0 : bundle.hashCode());
    return result;
  }
}
