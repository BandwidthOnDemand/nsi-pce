package net.es.nsi.pce.config.topo.nml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class models an NML Ethernet port.
 * 
 * @author hacksaw
 */
public class EthernetPort extends Port {
    private final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
    
    private Set<Integer> vlans = new LinkedHashSet<>();

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
        // Split the vlan first by comma, then by hyphen.
        Pattern pattern = Pattern.compile(",");
        String[] comma = pattern.split(vlans);
        for (int i = 0; i < comma.length; i++) {
            // Now by hyphen.
            pattern = Pattern.compile("-");
            String[] hyphen = pattern.split(comma[i]);

            // Just a single vlan.
            if (hyphen.length == 1) {
                this.vlans.add(Integer.parseInt(hyphen[0]));
            }
            // Two vlans in a range.
            else if (hyphen.length > 1 && hyphen.length < 3) {
                int min = Integer.parseInt(hyphen[0]);
                int max = Integer.parseInt(hyphen[1]);
                for (int j = min; j < max + 1; j++) {
                    this.vlans.add(j);
                }
            }
            // This is unsupported.
            else {
                throw new IllegalArgumentException("Invalid vlan string format: " + vlans);
            }
        }
    }
}
