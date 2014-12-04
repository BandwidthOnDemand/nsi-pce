/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.description.api;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public enum RestError {

    BAD_REQUEST(400, "BAD_REQUEST", "Client has made a bad request."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "Client must be authorized to make this request."),
    FORBIDDEN(403, "FORBIDDEN", "Client is forbidden from accessing target resource."),
    NOT_FOUND(404, "NOT_FOUND", "Target resource was not found."),
    CONFLICT(409, "CONFLICT", "Resource already exists."),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "Client has specified an unsupported media type."),
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "Client request contained unprocessable entity."),

    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "An internal server error has occured."),
    NOT_IMPLEMENTED(501, "NOT_IMPLEMENTED", "Requested action is not implemented."),
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "Requested service is unavailable."),

    // Mark the end.
    END(9000, "", "");

    protected int code;
    private String label;
    private String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, RestError> codeToStatusMapping;

    private RestError(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public static RestError getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (RestError s : values()) {
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

    public static String serialize(RestError error, String resource, String parameter) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", error.getCode());
        result.put("label", error.getLabel());
        result.put("description", error.getDescription());

        if (resource != null && !resource.isEmpty()) {
            result.put("resource", resource);
        }

        if (parameter != null && !parameter.isEmpty()) {
            result.put("parameter", resource);
        }

        Map<String, Object> jsonHolder = new HashMap<>();
        jsonHolder.put("error", result);
        Gson gson = new Gson();
        return gson.toJson(jsonHolder);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RestError = ");
        sb.append("{ code=").append(code);
        sb.append(", label='").append(label).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
