package net.es.nsi.pce.config.topo.nml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class EthernetPort extends Port {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
    
    private Set<Integer> vlans = new LinkedHashSet<Integer>();

    /**
     * @return the vlans
     */
    public Set<Integer> getVlans() {
        return vlans;
    }

    /**
     * @param vlans the vlans to set
     */
    public void setVlans(Set<Integer> vlans) {
        this.vlans = vlans;
    }
    
    public void addVlan(int vlan) {
        this.vlans.add(vlan);
    }
    
    public void removeVlan(int vlan) {
        this.vlans.remove(Integer.valueOf(vlan));
    }
    
    public static boolean isVlanLabel(String label) {
        return VLAN_LABEL.equalsIgnoreCase(label);
    }
    
    public void addVlanFromString(String vlans) {
        log.debug("addVlanFromString: converting vlans string " + vlans);
        
        // Split the vlan first by comma, then by hyphen.
        Pattern pattern = Pattern.compile(",");
        String[] comma = pattern.split(vlans);
        for (int i = 0; i < comma.length; i++) {
            log.debug("Pattern split: " + comma[i]);
            
            // Now by hyphen.
            pattern = Pattern.compile("-");
            String[] hyphen = pattern.split(comma[i]);
            log.debug("Length = " + hyphen.length);
            
            // Just a single vlan.
            if (hyphen.length == 1) {
                log.debug("Adding single vlan value = " + hyphen[0]);
                this.vlans.add(Integer.parseInt(hyphen[0]));
            }
            // Two vlans in a range.
            else if (hyphen.length > 1 && hyphen.length < 3) {
                int min = Integer.parseInt(hyphen[0]);
                int max = Integer.parseInt(hyphen[1]);
                log.debug("vlan min = " + min + ", vlan max = " + max);
                for (int j = min; j < max + 1; j++) {
                    log.debug("Adding vlan from list vlan = " + j);
                    this.vlans.add(j);
                }
            }
            // This is unsupported.
            else {
                log.error("Invalid vlan string format");
                throw new IllegalArgumentException("Invalid vlan string format");
            }
        }
    }
}
