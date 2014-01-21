/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.logs;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public enum PceLogs {
    AUDIT_START(1, "AUDIT_START", "Topology audit has started."),
    AUDIT_SUCCESSFUL(2, "AUDIT_SUCCESSFUL", "Topology audit has completed successfully."),
    AUDIT_PARTIAL(3, "AUDIT_PARTIAL", "Topology audit completed partially successful.");
    
    private int code;
    private String label;
    private String description;
 
    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, PceLogs> codeToStatusMapping;
 
    private PceLogs(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }
 
    public static PceLogs getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }
 
    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (PceLogs s : values()) {
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
