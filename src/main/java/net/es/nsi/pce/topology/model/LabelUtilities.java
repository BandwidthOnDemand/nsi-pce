/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.es.nsi.pce.topology.jaxb.NmlLabelGroupType;
import net.es.nsi.pce.topology.jaxb.NmlLabelType;
import net.es.nsi.pce.topology.jaxb.ObjectFactory;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.TypeValueType;

/**
 *
 * @author hacksaw
 */
public class LabelUtilities {
    private final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";

    private static ObjectFactory factory = new ObjectFactory();

    private static Comparator<TypeValueType> compareTypeValueType = new Comparator<TypeValueType>() {
        @Override
        public int compare(TypeValueType t1, TypeValueType t2) {
            if (t1 == null || t1.getType() == null || t2 == null || t2.getType() == null) {
                throw new IllegalArgumentException();
            }

            String tt1 = t1.getType();
            String tt2 = t2.getType();

            int type = tt1.compareTo(tt2);
            if (type != 0) {
                return type;
            }

            int tv1;
            if (t1.getValue() == null) {
                tv1 = 0;
            }
            else {
                tv1 = Integer.parseInt(t1.getValue());
            }

            int tv2;
            if (t2.getValue() == null) {
                tv2 = 0;
            }
            else {
                tv2 = Integer.parseInt(t2.getValue());
            }

            return (tv1 > tv2 ? -1 : (tv1 == tv2 ? 0 : 1));
        }
    };

    public static boolean isVlanLabel(String label) {
        return VLAN_LABEL.equalsIgnoreCase(label);
    }

    public static TypeValueType nmlLabelGroupToLabel(NmlLabelGroupType labelGroup) {
        // We only support the VLAN label at the moment.
        if (isVlanLabel(labelGroup.getLabeltype())) {
            TypeValueType label = factory.createTypeValueType();
            label.setType(labelGroup.getLabeltype());
            label.setValue(labelGroup.getValue());
            return label;
        }

        throw new IllegalArgumentException(labelGroup.getLabeltype());
    }

    public static TypeValueType nmlLabelToLabel(NmlLabelType nmlLabel) {

        if (isVlanLabel(nmlLabel.getLabeltype())) {
            TypeValueType label = factory.createTypeValueType();
            label.setType(nmlLabel.getLabeltype());
            label.setValue(nmlLabel.getValue());
            return label;
        }

        throw new IllegalArgumentException(nmlLabel.getLabeltype());
    }

    public static List<TypeValueType> parseEthernetVLAN(NmlLabelGroupType labelGroup) {
        List<TypeValueType> labels = new ArrayList<>();

        // Split the vlan first by comma, then by hyphen.
        Pattern pattern = Pattern.compile(",");
        String[] comma = pattern.split(labelGroup.getValue());
        for (int i = 0; i < comma.length; i++) {
            // Now by hyphen.
            pattern = Pattern.compile("-");
            String[] hyphen = pattern.split(comma[i]);

            // Just a single vlan.
            if (hyphen.length == 1) {
                TypeValueType label = factory.createTypeValueType();
                label.setType(labelGroup.getLabeltype());
                label.setValue(hyphen[0]);
                labels.add(label);
            }
            // Two vlans in a range.
            else if (hyphen.length > 1 && hyphen.length < 3) {
                int min = Integer.parseInt(hyphen[0]);
                int max = Integer.parseInt(hyphen[1]);
                for (int j = min; j < max + 1; j++) {
                    TypeValueType label = factory.createTypeValueType();
                    label.setType(labelGroup.getLabeltype());
                    label.setValue(Integer.toString(j));
                    labels.add(label);
                }
            }
            // This is unsupported.
            else {
                throw new IllegalArgumentException("Invalid vlan string format: " + labelGroup.getValue());
            }
        }

        return sortLabels(labels);
    }

    public static List<TypeValueType> sortLabels(List<TypeValueType> labels) {
        return Ordering.from(compareTypeValueType).sortedCopy(labels);
    }

    /**
     * Compares two ordered set of labels for equality.
     *
     * @param l1
     * @param l2
     * @return
     */
    public static boolean equals(List<TypeValueType> l1, List<TypeValueType> l2) {
        // Do the number of labels match?
        if (l1.size() != l2.size()) {
            return false;
        }

        // For each label type in the list compare type and values.
        for (int i = 0; i < l1.size(); i++) {
            String t1 = l1.get(i).getType();
            String t2 = l2.get(i).getType();

            if(t1 == null || t2 == null || !t1.equalsIgnoreCase(t2)) {
                return false;
            }

            String tv1 = l1.get(i).getValue();
            String tv2 = l2.get(i).getValue();

            if (tv1 == null && tv2 == null) {
                return true;
            }
            else if (tv1 == null) {
                return false;
            }
            else if (tv2 == null) {
                return false;
            }

            if (!tv1.equalsIgnoreCase(tv2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if v1 contains the value(s) contained in v2.  Assumes values
     * are strings representing integer ranges.
     *
     * @param v1
     * @param v2
     * @return
     */
    public static boolean contains(String v1, String v2) {
        if ((v1 == null || v1.isEmpty()) && (v2 == null || v2.isEmpty())) {
            return true;
        }
        else if (v1 == null || v1.isEmpty()) {
            return false;
        }
        else if (v2 == null || v2.isEmpty()) {
            return false;
        }

        // Now we convert the supplied values into Sets Lists.
        Set<Integer> vs1 = stringToIntegerSet(v1);
        Set<Integer> vs2 = stringToIntegerSet(v2);
        if (!vs1.containsAll(vs2)) {
                return false;
        }

        return true;
    }

    public static Set<Integer> stringToIntegerSet(String values) {
        Set<Integer> labels = new HashSet<>();

        if (values == null || values.isEmpty()) {
            return labels;
        }

        // Split the vlan first by comma, then by hyphen.
        Pattern pattern = Pattern.compile(",");
        String[] comma = pattern.split(values);
        for (int i = 0; i < comma.length; i++) {
            // Now by hyphen.
            pattern = Pattern.compile("-");
            String[] hyphen = pattern.split(comma[i]);

            // Just a single vlan.
            if (hyphen.length == 1) {
                labels.add(Integer.parseInt(hyphen[0].trim()));
            }
            // Two vlans in a range.
            else if (hyphen.length > 1 && hyphen.length < 3) {
                int min = Integer.parseInt(hyphen[0].trim());
                int max = Integer.parseInt(hyphen[1].trim());
                for (int j = min; j < max + 1; j++) {
                    labels.add(j);
                }
            }
            // This is unsupported.
            else {
                throw new IllegalArgumentException("Invalid string format: " + values);
            }
        }

        return labels;
    }

    public static boolean contains(StpType stp, String labelType, String labelValue) {
        // We only understand "vlan" at the moment.
        if (!"vlan".equalsIgnoreCase(labelType)) {
            return false;
        }

        for (TypeValueType lables : stp.getLabels()) {
            if (VLAN_LABEL.equalsIgnoreCase(lables.getType())) {
                Set<Integer> stpValues = stringToIntegerSet(lables.getValue());
                Set<Integer> values = stringToIntegerSet(labelValue);
                if (stpValues.containsAll(values)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean containsVLAN(StpType stp, String vlan) {
        for (TypeValueType lables : stp.getLabels()) {
            if (VLAN_LABEL.equalsIgnoreCase(lables.getType())) {
                Set<Integer> stpValues = stringToIntegerSet(lables.getValue());
                Set<Integer> values = stringToIntegerSet(vlan);
                if (stpValues.containsAll(values)) {
                    return true;
                }
            }
        }

        return false;
    }
}
