package net.es.nsi.pce.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.schema.DomParser;
import net.es.nsi.pce.test.TestConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * The REST API as its clients see it: status codes, content types and bodies of every endpoint, and
 * the FindPath callback, against the synthetic topology in topology/api.xml.
 *
 * Talks plain HTTP through the JDK, not through Jersey or MOXy, so it checks the server and not a
 * client built from the same libraries. Each response is compared with a golden file in
 * src/test/resources/golden. Bodies are compared after normalising: JSON by value with sorted keys,
 * XML by namespace URI rather than prefix, timestamps of the run replaced by a placeholder. Run
 * with -Dgolden.update=true to rewrite the golden files instead of comparing.
 */
public class ApiCharacterizationTest {
    private static final String PCE = "http://localhost:8400/";
    private static final int CALLBACK_PORT = 8492;
    private static final String CALLBACK = "http://localhost:" + CALLBACK_PORT + "/callback";
    private static final Path GOLDEN = Paths.get("src/test/resources/golden");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private static final String EVTS = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
    private static final String AGGREGATOR = "urn:ogf:network:aggregator.example.net:2026:nsa";
    private static final String STP = "urn:ogf:network:%s.example.net:2026:topology:%s";

    // Timestamps that depend on when the test runs rather than on what it asks.
    private static final Set<String> VOLATILE = Set.of(
            "discovered", "lastDiscovered", "lastAudit", "lastSuccessfulAudit", "lastAuditDuration", "nextExecution");

    private static TestConfig testConfig;
    private static HttpServer callbackServer;
    private static final BlockingQueue<Reply> CALLBACKS = new LinkedBlockingQueue<>();

    @BeforeClass
    public static void start() throws IOException {
        callbackServer = HttpServer.create(new InetSocketAddress("localhost", CALLBACK_PORT), 0);
        callbackServer.createContext("/callback", exchange -> {
            CALLBACKS.add(new Reply(0, exchange.getRequestHeaders().getFirst("Content-Type"), null,
                    read(exchange.getRequestBody())));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        callbackServer.start();
        testConfig = new TestConfig("src/test/resources/config/topology-api.xml");
    }

    @AfterClass
    public static void stop() {
        testConfig.shutdown();
        callbackServer.stop(0);
    }

    // FindPath, as Safnari sends it: JSON, SEQUENTIAL, with the trace and an empty constraint list.
    @Test
    public void findPathSequentialJson() throws Exception {
        String request = "{\n"
                + "  \"correlationId\": \"urn:uuid:00000000-0000-4000-8000-000000000001\",\n"
                + "  \"nsaId\": \"" + AGGREGATOR + "\",\n"
                + "  \"replyTo\": {\"url\": \"" + CALLBACK + "\", \"mediaType\": \"application/json\"},\n"
                + "  \"algorithm\": \"SEQUENTIAL\",\n"
                + "  \"startTime\": \"2030-01-01T00:00:00.000Z\",\n"
                + "  \"endTime\": \"2030-01-01T01:00:00.000Z\",\n"
                + "  \"constraints\": [],\n"
                + "  \"serviceType\": \"" + EVTS + "\",\n"
                + "  \"p.p2ps\": [{\n"
                + "    \"capacity\": 100,\n"
                + "    \"directionality\": \"Bidirectional\",\n"
                + "    \"symmetricPath\": true,\n"
                + "    \"sourceSTP\": \"" + stp("domain-a", "a-edge?vlan=1000") + "\",\n"
                + "    \"destSTP\": \"" + stp("domain-b", "b-edge?vlan=1050") + "\",\n"
                + "    \"parameter\": [{\"@type\": \"protection\", \"value\": \"PROTECTED\"}]\n"
                + "  }],\n"
                + "  \"trace\": [{\"index\": 0, \"value\": \"urn:ogf:network:requester.example.net:2026:nsa\"}]\n"
                + "}";
        findPath("find-sequential-json", "application/json", request);
    }

    @Test
    public void findPathTreeXml() throws Exception {
        findPath("find-tree-xml", "application/xml", xmlRequest("00000000-0000-4000-8000-000000000002",
                "application/xml", "TREE", stp("domain-a", "a-edge?vlan=1000"), stp("domain-b", "b-edge?vlan=1050"), ""));
    }

    @Test
    public void findPathVersionedXmlWithEro() throws Exception {
        String ero = "<ero><orderedSTP order=\"0\"><stp>" + stp("domain-a", "a-to-b") + "</stp></orderedSTP></ero>";
        findPath("find-ero-versioned-xml", "application/vnd.net.es.pce.v1+xml",
                xmlRequest("00000000-0000-4000-8000-000000000003", "application/vnd.net.es.pce.v1+xml", "SEQUENTIAL",
                        stp("domain-a", "a-edge?vlan=1000"), stp("domain-b", "b-edge?vlan=1050"), ero));
    }

    @Test
    public void findPathUnknownStpReportsFailure() throws Exception {
        findPath("find-unknown-stp-json", "application/xml", xmlRequest("00000000-0000-4000-8000-000000000004",
                "application/json", "TREE", stp("domain-a", "nowhere?vlan=1000"), stp("domain-b", "b-edge?vlan=1050"), ""));
    }

    @Test
    public void findPathWithoutReplyToIsRejected() throws Exception {
        String request = "{\"correlationId\": \"urn:uuid:00000000-0000-4000-8000-000000000005\","
                + " \"serviceType\": \"" + EVTS + "\"}";
        Reply reply = call("POST", "paths/find", "application/json", "application/json", request);
        compare("find-without-replyto", reply, null);
    }

    @Test
    public void findPathWithUnsupportedMediaTypeIsRejected() throws Exception {
        Reply reply = call("POST", "paths/find", "application/xml", "application/xml", xmlRequest(
                "00000000-0000-4000-8000-000000000006", "text/plain", "TREE",
                stp("domain-a", "a-edge?vlan=1000"), stp("domain-b", "b-edge?vlan=1050"), ""));
        compare("find-unsupported-mediatype", reply, null);
    }

    @Test
    public void getEndpoints() throws Exception {
        String[][] endpoints = {
            {"paths", "paths", "application/json"},
            {"reachability", "reachability", "application/json"},
            {"topology", "topology", "application/json"},
            {"topology-ping", "topology/ping", "application/json"},
            {"topology-stps", "topology/stps", "application/json"},
            {"topology-stp", "topology/stps/" + stp("domain-b", "b-edge?vlan=1050"), "application/json"},
            {"topology-networks", "topology/networks", "application/json"},
            {"topology-network-stps", "topology/networks/urn:ogf:network:domain-a.example.net:2026:topology/stps",
                    "application/vnd.org.ogf.nsi.topology.v1+json"},
            {"topology-nsas", "topology/nsas", "application/json"},
            {"topology-service-domains", "topology/serviceDomains", "application/json"},
            {"topology-sdps", "topology/sdps", "application/json"},
            {"topology-services", "topology/services", "application/json"},
            {"topology-unknown-network", "topology/networks/urn:ogf:network:nowhere", "application/json"},
            {"descriptions", "descriptions", "application/json"},
            {"management-status-json", "management/status/topology", "application/json"},
            {"management-status-xml", "management/status/topology", "application/xml"},
            {"management-timers", "management/timers", "application/json"},
            {"www", "www/index.html", "text/html"},
        };
        for (String[] endpoint : endpoints) {
            compare("get-" + endpoint[0], call("GET", endpoint[1].replace("?", "%3F"), null, endpoint[2], null), null);
        }
    }

    @Test
    public void ifModifiedSince() throws Exception {
        String[][] cases = {
            {"before", "Sat, 01 Jan 2000 00:00:00 GMT"},
            {"after", "Fri, 01 Jan 2100 00:00:00 GMT"},
            {"after-rfc1036", "Friday, 01-Jan-99 00:00:00 GMT"},
        };
        // "before" on topology is a 500: SDPs carry no discovered time, which the filter dereferences.
        for (String[] c : cases) {
            for (String path : Arrays.asList("topology", "topology/stps", "descriptions")) {
                Reply reply = call("GET", path, null, "application/json", null, "If-Modified-Since", c[1]);
                compare("if-modified-since-" + c[0] + "-" + path.replace('/', '-'), reply, null);
            }
        }
    }

    private static void findPath(String name, String contentType, String request) throws Exception {
        CALLBACKS.clear();
        Reply reply = call("POST", "paths/find", contentType, contentType, request);
        Reply callback = null;
        if (reply.status() == 202) {
            callback = CALLBACKS.poll(30, TimeUnit.SECONDS);
            assertNotNull(name + ": no callback received", callback);
        }
        compare(name, reply, callback);
    }

    private static String xmlRequest(String id, String mediaType, String algorithm, String source, String dest,
            String ero) {
        return "<m:findPathRequest xmlns:m=\"http://schemas.es.net/nsi/2013/08/pce/messages\""
                + " xmlns:p=\"http://schemas.ogf.org/nsi/2013/12/services/point2point\">"
                + "<correlationId>urn:uuid:" + id + "</correlationId>"
                + "<replyTo><url>" + CALLBACK + "</url><mediaType>" + mediaType + "</mediaType></replyTo>"
                + "<algorithm>" + algorithm + "</algorithm>"
                + "<startTime>2030-01-01T00:00:00.000Z</startTime><endTime>2030-01-01T01:00:00.000Z</endTime>"
                + "<serviceType>" + EVTS + "</serviceType>"
                + "<p:p2ps><capacity>100</capacity><directionality>Bidirectional</directionality>"
                + "<symmetricPath>true</symmetricPath><sourceSTP>" + source + "</sourceSTP>"
                + "<destSTP>" + dest + "</destSTP>" + ero + "</p:p2ps>"
                + "</m:findPathRequest>";
    }

    private static String stp(String domain, String port) {
        return String.format(STP, domain, port);
    }

    private static Reply call(String method, String path, String contentType, String accept, String body,
            String... headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(PCE + path).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", accept);
        for (int i = 0; i < headers.length; i += 2) {
            connection.setRequestProperty(headers[i], headers[i + 1]);
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = connection.getResponseCode();
        InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String content = in == null ? "" : read(in);
        return new Reply(status, connection.getContentType(), connection.getHeaderField("Last-Modified"), content);
    }

    private static String read(InputStream in) throws IOException {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void compare(String name, Reply reply, Reply callback) throws Exception {
        StringBuilder actual = new StringBuilder();
        actual.append(reply.describe("response"));
        if (callback != null) {
            actual.append(callback.describe("callback"));
        }
        Path golden = GOLDEN.resolve(name + ".txt");
        if (UPDATE) {
            Files.createDirectories(GOLDEN);
            Files.writeString(golden, actual);
            return;
        }
        assertEquals(name, Files.readString(golden), actual.toString());
    }

    /** status 0 for a callback, which is a request rather than a response. */
    private record Reply(int status, String contentType, String lastModified, String body) {
        String describe(String what) throws Exception {
            StringBuilder out = new StringBuilder();
            out.append("== ").append(what).append('\n');
            if (status != 0) {
                out.append("status: ").append(status).append('\n');
            }
            out.append("content-type: ").append(contentType).append('\n');
            if (lastModified != null) {
                out.append("last-modified: present\n");
            }
            out.append(normalise(contentType, body)).append('\n');
            return out.toString();
        }
    }

    private static String normalise(String contentType, String body) throws Exception {
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return "(empty)";
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return new GsonBuilder().setPrettyPrinting().create()
                    .toJson(normaliseJson(JsonParser.parseString(trimmed)));
        }
        if (trimmed.startsWith("<") && contentType != null && contentType.contains("xml")) {
            StringBuilder out = new StringBuilder();
            normaliseXml(DomParser.xml2Dom(trimmed).getDocumentElement(), "", out);
            return out.toString().trim();
        }
        // Grizzly's default error page names its version.
        return trimmed.replaceAll("Grizzly [0-9.]+", "Grizzly");
    }

    /** Sorted keys, volatile values replaced, arrays of objects in a stable order. */
    private static JsonElement normaliseJson(JsonElement element) {
        if (element.isJsonObject()) {
            Map<String, JsonElement> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                sorted.put(entry.getKey(), VOLATILE.contains(entry.getKey())
                        ? new JsonPrimitive("<volatile>") : normaliseJson(entry.getValue()));
            }
            JsonObject object = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : sorted.entrySet()) {
                object.add(entry.getKey(), entry.getValue());
            }
            return object;
        }
        if (element.isJsonArray()) {
            List<JsonElement> items = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                items.add(normaliseJson(item));
            }
            // Topology collections come out of hash maps; a path keeps its order.
            if (!items.isEmpty() && items.get(0).isJsonObject() && !items.get(0).getAsJsonObject().has("csProviderURL")) {
                items.sort(Comparator.comparing(JsonElement::toString));
            }
            JsonArray array = new JsonArray();
            for (JsonElement item : items) {
                array.add(item);
            }
            return array;
        }
        return element;
    }

    /** One line per element and attribute, named by namespace URI; prefixes do not matter. */
    private static void normaliseXml(Element element, String indent, StringBuilder out) {
        String name = "{" + nullToEmpty(element.getNamespaceURI()) + "}" + element.getLocalName();
        out.append(indent).append(name);
        NamedNodeMap attributes = element.getAttributes();
        Map<String, String> sorted = new TreeMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            if ("http://www.w3.org/2000/xmlns/".equals(attr.getNamespaceURI())) {
                continue;
            }
            String value = attr.getValue();
            // xsi:type names a type by prefix; resolve it.
            if ("type".equals(attr.getLocalName())
                    && "http://www.w3.org/2001/XMLSchema-instance".equals(attr.getNamespaceURI()) && value.contains(":")) {
                String[] qname = value.split(":", 2);
                value = "{" + nullToEmpty(element.lookupNamespaceURI(qname[0])) + "}" + qname[1];
            }
            sorted.put("{" + nullToEmpty(attr.getNamespaceURI()) + "}" + attr.getLocalName(),
                    VOLATILE.contains(attr.getLocalName()) ? "<volatile>" : value);
        }
        sorted.forEach((key, value) -> out.append(' ').append(key).append("=\"").append(value).append('"'));
        out.append('\n');
        if (VOLATILE.contains(element.getLocalName())) {
            out.append(indent).append("  <volatile>\n");
            return;
        }
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                normaliseXml((Element) child, indent + "  ", out);
            } else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String text = child.getNodeValue().trim();
                if (!text.isEmpty()) {
                    out.append(indent).append("  \"").append(text).append("\"\n");
                }
            }
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
