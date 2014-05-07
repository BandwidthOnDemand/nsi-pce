/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author hacksaw
 */
public class SimpleLabel {
    private String type;
    private String value;

    public SimpleLabel() {

    }

    public SimpleLabel(String type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return type + "=" + value;
    }

    @Override
    public boolean equals(Object object){
        if (object == this) {
            return true;
        }

        if((object == null) || (object.getClass() != this.getClass())) {
            return false;
        }

        SimpleLabel that = (SimpleLabel) object;
        if (this.type == null && that.getType() == null) {
            return true;
        }

        if (this.type == null && that.getType() != null) {
            return false;
        }

        if (this.type != null && that.getType() == null) {
            return false;
        }

        if (!this.type.equalsIgnoreCase(this.getType())) {
            return false;
        }

        if (this.value == null && that.getValue() == null) {
            return true;
        }

        if (this.value == null && that.getValue() != null) {
            return false;
        }

        if (this.value != null && that.getValue() == null) {
            return false;
        }

        if (!this.value.equalsIgnoreCase(this.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((type == null) ? 0 : type.hashCode());
        result = prime * result
                + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    private static Pattern equalsPattern = Pattern.compile("=");
    private static Pattern commaPattern = Pattern.compile(",");
    private static Pattern hyphenPattern = Pattern.compile("-");

    public static Set<SimpleLabel> getSimpleLabels(String labels) {
        Set<SimpleLabel> results = new HashSet<>();

        if (labels == null || labels.isEmpty()) {
            return results;
        }

        // Split the vlan first by comma, then by hyphen.
        String[] equals = equalsPattern.split(labels);
        if (equals.length == 1 && !equals[0].isEmpty()) {
            SimpleLabel label = new SimpleLabel();
            label.setType(equals[0].trim());
            results.add(label);
        }
        else if (equals.length > 1 && !equals[1].isEmpty()) {
            String type = equals[0].trim();
            // Split the vlan first by comma, then by hyphen.
            String[] comma = commaPattern.split(equals[1]);
            for (int i = 0; i < comma.length; i++) {
                // Now by hyphen.
                String[] hyphen = hyphenPattern.split(comma[i]);

                // Just a single vlan.
                if (hyphen.length == 1) {
                    SimpleLabel label = new SimpleLabel();
                    label.setType(type);
                    label.setValue(hyphen[0].trim());
                    results.add(label);
                }
                // Two vlans in a range.
                else if (hyphen.length > 1 && hyphen.length < 3) {
                    int min = Integer.parseInt(hyphen[0].trim());
                    int max = Integer.parseInt(hyphen[1].trim());
                    for (int j = min; j < max + 1; j++) {
                        SimpleLabel label = new SimpleLabel();
                        label.setType(type);
                        label.setValue(Integer.toString(j));
                        results.add(label);
                    }
                }
                // This is unsupported.
                else {
                    throw new IllegalArgumentException("Invalid string format: " + labels);
                }
            }
        }

        return results;
    }
}
