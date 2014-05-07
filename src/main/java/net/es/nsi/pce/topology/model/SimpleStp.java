/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author hacksaw
 */
public class SimpleStp {
    private String id;
    private Set<SimpleLabel> labels = new HashSet<>();

    private static Pattern questionPattern = Pattern.compile("\\?");

    public SimpleStp() {
    }

    public SimpleStp(String id, Set<SimpleLabel> labels) {
        this.id = id;
        this.labels = labels;
    }

    public SimpleStp(String id, SimpleLabel label) {
        this.id = id;
        this.labels.add(label);
    }

    public SimpleStp(String stpId) {
        if (stpId == null || stpId.isEmpty()) {
            // An empty string gets an emtpy STP.
            return;
        }

        // If a question mark is present then we have to process attached label.
        String[]  question = questionPattern.split(stpId);
        this.id = question[0];
        if (question.length > 1) {
            // We need to parse the label.
            this.labels = SimpleLabel.getSimpleLabels(question[1]);
        }
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    public String getStpId() {
        StringBuilder sb = new StringBuilder(id);

        if (labels != null && !labels.isEmpty()) {
            SimpleLabel label = labels.iterator().next();
            sb.append("?");
            sb.append(label.getType());
            sb.append("=");
            sb.append(label.getValue());
        }
        
        return sb.toString();
    }

    /**
     * @return the labels
     */
    public Set<SimpleLabel> getLabels() {
        return Collections.unmodifiableSet(labels);
    }

    /**
     * @param labels the labels to set
     */
    public void setLabels(Set<SimpleLabel> labels) {
        this.labels.clear();
        this.getLabels().addAll(labels);
    }

    public void addLabels(SimpleLabel label) {
        this.labels.add(label);
    }

    public static SimpleStp getSimpleStp(String stpId) {
        SimpleStp simpleStp = new SimpleStp();

        if (stpId == null || stpId.isEmpty()) {
            // An empty string gets an emtpy STP.
            return simpleStp;
        }

        // If a question mark is present then we have to process attached label.
        String[]  question = questionPattern.split(stpId);
        simpleStp.setId(question[0]);
        if (question.length > 1) {
            // We need to parse the label.
            simpleStp.setLabels(SimpleLabel.getSimpleLabels(question[1]));
        }

        return simpleStp;
    }
}
