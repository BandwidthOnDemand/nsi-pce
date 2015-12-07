/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.es.nsi.pce.jaxb.topology.NmlLabelGroupType;
import net.es.nsi.pce.jaxb.topology.NmlLabelType;

/**
 *
 * @author hacksaw
 */
public class NmlEthernet {
    public final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
    final static String VLAN = "vlan";

    public static boolean isVlanLabel(Optional<String> label) {
        if (!label.isPresent()) {
            return false;
        }
        return VLAN_LABEL.equalsIgnoreCase(label.get());
    }

    public static Set<NmlLabelType> labelGroupToLabels(NmlLabelGroupType labelGroup) {

        Optional<String> labelType = Optional.ofNullable(labelGroup.getLabeltype());
        if (!isVlanLabel(labelType)) {
            throw new IllegalArgumentException("Invalid vlan label: " + labelGroup.getLabeltype());
        }

        Set<NmlLabelType> labels = new LinkedHashSet<>();

        // Split the vlan first by comma, then by hyphen.
        Pattern pattern = Pattern.compile(",");
        String[] comma = pattern.split(labelGroup.getValue());
        for (int i = 0; i < comma.length; i++) {
            // Now by hyphen.
            pattern = Pattern.compile("-");
            String[] hyphen = pattern.split(comma[i]);

            // Just a single vlan.
            if (hyphen.length == 1) {
                NmlLabelType label = new NmlLabelType();
                label.setLabeltype(labelGroup.getLabeltype());
                label.setValue(hyphen[0]);
                labels.add(label);
            }
            // Two vlans in a range.
            else if (hyphen.length > 1 && hyphen.length < 3) {
                int min = Integer.parseInt(hyphen[0]);
                int max = Integer.parseInt(hyphen[1]);
                for (int j = min; j < max + 1; j++) {
                    NmlLabelType label = new NmlLabelType();
                    label.setLabeltype(labelGroup.getLabeltype());
                    label.setValue(Integer.toString(j));
                    labels.add(label);
                }
            }
            // This is unsupported.
            else {
                throw new IllegalArgumentException("Invalid vlan string format: " + labelGroup.getValue());
            }
        }
        return labels;
    }
}
