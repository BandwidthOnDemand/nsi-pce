/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import org.glassfish.jersey.client.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class JsonProxy {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Gson serializer;

    public JsonProxy() {
        GsonBuilder gson = new GsonBuilder();
        // Hierarchy adapters, so they also match the JDK's implementation classes: Gson can no longer
        // reflect into the java.xml module. One registration per type, as a second replaces the first.
        gson.registerTypeHierarchyAdapter(XMLGregorianCalendar.class, new XMLGregorianCalendarAdapter());
        gson.registerTypeHierarchyAdapter(QName.class, new QNameAdapter());
        gson.setExclusionStrategies(new JsonExclusionStrategy());
        serializer = gson.create();
    }

    public String serialize(Object obj) {
        return serializer.toJson(obj);
    }

    public <T extends Object> T deserialize(String json, Class<T> classOfT) {
        return serializer.fromJson(json, classOfT);
    }

    public <T extends Object> T deserialize(Response response, Class<T> classOfT) {
        final ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {});
        String chunk;
        StringBuilder buffer = new StringBuilder();
        while ((chunk = chunkedInput.read()) != null) {
            buffer.append(chunk);
        }
        response.close();
        return serializer.fromJson(buffer.toString(), classOfT);
    }

    public <T extends Object> String serializeList(Object obj, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.toJson(obj, type);
    }

    public <T extends Object> List<T> deserializeList(String json, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.fromJson(json, type);
    }

    public <T extends Object> List<T> deserializeList(Response response, Class<T> classOfT) {
        final ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {});
        String chunk;
        StringBuilder buffer = new StringBuilder();
        while ((chunk = chunkedInput.read()) != null) {
            buffer.append(chunk);
        }
        response.close();
        return deserializeList(buffer.toString(), classOfT);
    }

    private static class ListParameterizedType implements ParameterizedType {

        private final Type type;

        private ListParameterizedType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] {type};
        }

        @Override
        public Type getRawType() {
            return ArrayList.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 83 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (this.getClass() != other.getClass()) return false;
            ListParameterizedType otherType = (ListParameterizedType) other;
            return (this.type == otherType.type);
        }
    }

    private class XMLGregorianCalendarAdapter implements JsonSerializer<XMLGregorianCalendar>, JsonDeserializer<XMLGregorianCalendar> {
        @Override
        public JsonElement serialize(XMLGregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public XMLGregorianCalendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return DatatypeFactory.newInstance().newXMLGregorianCalendar(json.getAsJsonPrimitive().getAsString());
            } catch (DatatypeConfigurationException ex) {
                log.error("XMLGregorianCalendarDeserializer: Could not convert supplied date " +  json.getAsJsonPrimitive().getAsString());
                return(null);
            }
        }
    }

    /**
     * The JSON Gson wrote by reflection before Java 9 closed the java.xml module to it. Map keys
     * (otherAttributes) still use QName.toString().
     */
    private static class QNameAdapter implements JsonSerializer<QName>, JsonDeserializer<QName> {
        @Override
        public JsonElement serialize(QName src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("namespaceURI", src.getNamespaceURI());
            json.addProperty("localPart", src.getLocalPart());
            json.addProperty("prefix", src.getPrefix());
            return json;
        }

        @Override
        public QName deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return QName.valueOf(json.getAsString());
            }
            JsonObject object = json.getAsJsonObject();
            return new QName(object.get("namespaceURI").getAsString(), object.get("localPart").getAsString(),
                    object.get("prefix").getAsString());
        }
    }
}
