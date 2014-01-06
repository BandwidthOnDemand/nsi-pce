/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.provider;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public enum TopologyStatus {
    Error(-1, "Error", "A fatal error occured during processing."),
    Initializing(0, "Initializing", "The topology provider is initializing."),
    Auditing(1, "Auditing", "The topology provider is auditing network topology."),
    Completed(2, "Completed", "Topology has been successfully loaded."),
    PartiallyComplete(3, "PartiallyCompleted", "Partial topology has been loaded due to errors.");
    
    private int code;
    private String label;
    private String description;
 
    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, TopologyStatus> codeToStatusMapping;
 
    private TopologyStatus(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }
 
    public static TopologyStatus getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }
 
    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (TopologyStatus s : values()) {
            codeToStatusMapping.put(s.code, s);
        }
    }
 
    public int getCode() {
        return code;
    }
 
    public String getLabel() {
        return label;
    }
 
    public String getDescription() {
        return description;
    }
 
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TopologyProviderStatus");
        sb.append("{ code=").append(code);
        sb.append(", label='").append(label).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
