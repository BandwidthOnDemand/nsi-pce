package net.es.nsi.pce.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import net.es.nsi.pce.jaxb.dds.CollectionType;
import net.es.nsi.pce.jaxb.dds.DocumentType;
import net.es.nsi.pce.jaxb.topology.NmlTopologyType;
import net.es.nsi.pce.jaxb.topology.NsaNsaType;
import net.es.nsi.pce.schema.DdsParser;
import net.es.nsi.pce.schema.NmlParser;
import net.es.nsi.pce.schema.NsaParser;
import net.es.nsi.pce.schema.NsiConstants;
import org.apache.log4j.xml.DOMConfigurator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author hacksaw
 */
public class DecodeTest {
    private Logger log;

    @Before
    public void setUp() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException, IOException {
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/log4j.xml"), 45 * 1000);
        log = LoggerFactory.getLogger(DecodeTest.class);

    }

    @Test
    public void decodebase64AndZipTest() throws IOException, TransformerException, JAXBException {
        String encoded = "H4sIAAAAAAAAAO1YUW+bMBD+K4g+g4HAmlpJqkxrq0rRNi2sD3tz4EKsEhvZpl3//TA4lGSpmjWsUru94fPdd3efzd3B6PznOrfuQEjK2dj2Xc+2gCU8pSwb29/jS2don09GTA5wzAue8+zBqiyYxJVobK+UKjBCMlnBmkiXZ0uXiwyxdY4Czx8gL0ILIuHEbo2Cp40kNUZnSBlfHcPweUM/QBLEHU1AohSWlFFVZfUIET0XcIg8H4FagWCgbIumY7sUDFdauBLcc3GLU6CJm95i7RBvwrQfGazkkeMHjhfE/ikenOIwcoMo+GE3JDKyhonBGKFWUu/N6BIU3aykIkJNnoJrbBudWh1YqpU/1Mp+7Ad4MMTe0A2HA8eLsOc1Flqtedr29pGmVECi+SL5Vy7U4cnjZVntOxkQprpZdsS7mWoHV4KXxcu8OJTZqB8kXioNhfZycDw1c8UFyUBbdanpiPugpgN3JDVdpL9MDS1ALHeJaYV90NKCHUnKI86zlITYFKBPbfk53NF81xRf3MRzd+pcfZldaJY0G1ez60trWiq+JgpSS29ZF6ZiWTez6WcrFoTJJQjL4FXh1jSayOKHAiabMmiq5d5SmoJMBC10IBJ1IhmhLpImY0/WDdvfICc1B6pSPbxbrIj8UqoFL1nauSB9vOqm0JIF5A1Urh8PiS5Avte2hpO7nDC7Kc6BF0SmorawR2VP5TSnRB6UdZ2ZW6128tZsbNVK1A3ILFvo37aPO7tr1vfRVTn8gyfXrTavdHQ9vXa7beTVDu8N3OudZv0+qOnp3mx32n6p8Z3QOwvf5JXZGmTeAStmMDGpzO+pSlbV96YR//nAVM1ClGHiZDwHrECqBaS4GVYcPbjYDUfze1IU9XetEiUYYfxC4v4Pe91hD+07yP2ta/MbYfILgAP1OHsQAAA=";
        Document decoded = Decoder.decode("base64", "application/x-gzip", encoded);
        assertNotNull(decoded);

        NmlTopologyType dom2Nml = NmlParser.getInstance().dom2Nml(decoded);

        assertEquals("urn:ogf:network:deic.dk:2013:topology", dom2Nml.getId());
    }

    @Test
    public void decodebase64Test() throws IOException, TransformerException, JAXBException {
        String encoded = "PG5zYTpuc2EgaWQ9InVybjpvZ2Y6bmV0d29yazpuZXRoZXJsaWdodC5uZXQ6MjAxMzpuc2E6c2FmbmFyaSIgdmVyc2lvbj0iMjAxNS0xMi0wMVQxNzo0MjozMS4xNDMrMDE6MDAiIHhtbG5zOmducz0iaHR0cDovL25vcmR1Lm5ldC9uYW1lc3BhY2VzLzIwMTMvMTIvZ25zYm9kIiB4bWxuczpuc2E9Imh0dHA6Ly9zY2hlbWFzLm9nZi5vcmcvbnNpLzIwMTQvMDIvZGlzY292ZXJ5L25zYSIgeG1sbnM6dmNhcmQ9InVybjppZXRmOnBhcmFtczp4bWw6bnM6dmNhcmQtNC4wIj4NCiAgICAgIDxuYW1lPk5ldGhlckxpZ2h0IFNhZm5hcmk8L25hbWU+DQogICAgICA8c29mdHdhcmVWZXJzaW9uPjEuMC1TTkFQU0hPVCAoMDQyMzcyMyk8L3NvZnR3YXJlVmVyc2lvbj4NCiAgICAgIDxzdGFydFRpbWU+MjAxNS0xMS0xNlQyMTozOTo0Ny43NDgrMDE6MDA8L3N0YXJ0VGltZT4NCiAgICAgIDxhZG1pbkNvbnRhY3Q+DQogICAgICAgIDx2Y2FyZDp2Y2FyZD4NCiAgICAgICAgICA8dmNhcmQ6dWlkPg0KICAgICAgICAgICAgPHZjYXJkOnVyaT5odHRwczovL2FnZy5uZXRoZXJsaWdodC5uZXQvbnNpLXYyL0Nvbm5lY3Rpb25TZXJ2aWNlUHJvdmlkZXIjYWRtaW5Db250YWN0PC92Y2FyZDp1cmk+DQogICAgICAgICAgPC92Y2FyZDp1aWQ+DQogICAgICAgICAgPHZjYXJkOnByb2RpZD4NCiAgICAgICAgICAgIDx2Y2FyZDp0ZXh0PnNhZm5hcmkgPC92Y2FyZDp0ZXh0Pg0KICAgICAgICAgIDwvdmNhcmQ6cHJvZGlkPg0KICAgICAgICAgIDx2Y2FyZDpyZXY+DQogICAgICAgICAgICA8dmNhcmQ6dGltZXN0YW1wPjIwMTUxMTE2VDIxMzk0N1o8L3ZjYXJkOnRpbWVzdGFtcD4NCiAgICAgICAgICA8L3ZjYXJkOnJldj4NCiAgICAgICAgICA8dmNhcmQ6a2luZD4NCiAgICAgICAgICAgIDx2Y2FyZDp0ZXh0PmluZGl2aWR1YWw8L3ZjYXJkOnRleHQ+DQogICAgICAgICAgPC92Y2FyZDpraW5kPg0KICAgICAgICAgIDx2Y2FyZDpmbj4NCiAgICAgICAgICAgIDx2Y2FyZDp0ZXh0PkhhbnMgVHJvbXBlcnQ8L3ZjYXJkOnRleHQ+DQogICAgICAgICAgPC92Y2FyZDpmbj4NCiAgICAgICAgICA8dmNhcmQ6bj4NCiAgICAgICAgICAgIDx2Y2FyZDpzdXJuYW1lPlRyb21wZXJ0PC92Y2FyZDpzdXJuYW1lPg0KICAgICAgICAgICAgPHZjYXJkOmdpdmVuPkhhbnM8L3ZjYXJkOmdpdmVuPg0KICAgICAgICAgIDwvdmNhcmQ6bj4NCiAgICAgICAgPC92Y2FyZDp2Y2FyZD4NCiAgICAgIDwvYWRtaW5Db250YWN0Pg0KICAgICAgPGxvY2F0aW9uPg0KICAgICAgICA8bG9uZ2l0dWRlPjQuOTU0NTg1PC9sb25naXR1ZGU+DQogICAgICAgIDxsYXRpdHVkZT41Mi4zNTY3PC9sYXRpdHVkZT4NCiAgICAgIDwvbG9jYXRpb24+DQogICAgICANCiAgICAgIA0KICAgICAgPGludGVyZmFjZT4NCiAgICAgICAgPHR5cGU+YXBwbGljYXRpb24vdm5kLm9nZi5uc2kuZGRzLnYxK3htbDwvdHlwZT4NCiAgICAgICAgPGhyZWY+aHR0cHM6Ly9hZ2cubmV0aGVybGlnaHQubmV0L2RkczwvaHJlZj4NCiAgICAgIDwvaW50ZXJmYWNlPg0KICAgICAgPGludGVyZmFjZT4NCiAgICAgICAgPHR5cGU+YXBwbGljYXRpb24vdm5kLm9nZi5uc2kuY3MudjIucmVxdWVzdGVyK3NvYXA8L3R5cGU+DQogICAgICAgIDxocmVmPmh0dHBzOi8vYWdnLm5ldGhlcmxpZ2h0Lm5ldC9uc2ktdjIvQ29ubmVjdGlvblNlcnZpY2VSZXF1ZXN0ZXI8L2hyZWY+DQogICAgICA8L2ludGVyZmFjZT4NCiAgICAgIDxpbnRlcmZhY2U+DQogICAgICAgIDx0eXBlPmFwcGxpY2F0aW9uL3ZuZC5vZ2YubnNpLmNzLnYyLnByb3ZpZGVyK3NvYXA8L3R5cGU+DQogICAgICAgIDxocmVmPmh0dHBzOi8vYWdnLm5ldGhlcmxpZ2h0Lm5ldC9uc2ktdjIvQ29ubmVjdGlvblNlcnZpY2VQcm92aWRlcjwvaHJlZj4NCiAgICAgIDwvaW50ZXJmYWNlPg0KICAgICAgDQogICAgICA8ZmVhdHVyZSB0eXBlPSJ2bmQub2dmLm5zaS5jcy52Mi5yb2xlLmFnZ3JlZ2F0b3IiLz4NCiAgICAgIDxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOnN1cmZuZXQubmw6MTk5MDpuc2E6Ym9kNzwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOm5ldGhlcmxpZ2h0Lm5ldDoyMDEzOm5zYTpib2Q8L3BlZXJzV2l0aD48cGVlcnNXaXRoPnVybjpvZ2Y6bmV0d29yazpjemVjaGxpZ2h0LmNlc25ldC5jejoyMDEzOm5zYTwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOnV2YWxpZ2h0Lm5ldDoyMDEzOm5zYTwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOmljYWlyLm9yZzoyMDEzOm5zYTwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOmVzLm5ldDoyMDEzOm5zYTpuc2ktYWdnci13ZXN0PC9wZWVyc1dpdGg+PHBlZXJzV2l0aD51cm46b2dmOm5ldHdvcms6YWlzdC5nby5qcDoyMDEzOm5zYTwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOmFpc3QuZ28uanA6MjAxMzpuc2E6bnNpLWFnZ3I8L3BlZXJzV2l0aD48cGVlcnNXaXRoPnVybjpvZ2Y6bmV0d29yazpzb3V0aGVybmxpZ2h0Lm5ldC5icjoyMDEzOm5zYTwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOm5vcmR1Lm5ldDoyMDEzOm5zYTwvcGVlcnNXaXRoPjxwZWVyc1dpdGg+dXJuOm9nZjpuZXR3b3JrOmFtcGF0aC5uZXQ6MjAxMzpuc2E8L3BlZXJzV2l0aD48cGVlcnNXaXRoPnVybjpvZ2Y6bmV0d29yazpzdXJmbmV0Lm5sOjE5OTA6bnNhOmJvZC1hY2M8L3BlZXJzV2l0aD48cGVlcnNXaXRoPnVybjpvZ2Y6bmV0d29yazppY2Fpci5vcmc6MjAxMzpuc2E6bnNpLWFtLXNsPC9wZWVyc1dpdGg+PHBlZXJzV2l0aD51cm46b2dmOm5ldHdvcms6Z2VhbnQubmV0OjIwMTM6bnNhPC9wZWVyc1dpdGg+DQogICAgICANCiAgICA8L25zYTpuc2E+";
        Document decoded = Decoder.decode("base64", "application/xml", encoded);
        assertNotNull(decoded);

        NsaNsaType dom2Nsa = NsaParser.getInstance().dom2Nsa(decoded);

        assertEquals("urn:ogf:network:netherlight.net:2013:nsa:safnari", dom2Nsa.getId());
    }


    @Test(expected=IOException.class)
    public void decodeOldTest() throws JAXBException, RuntimeException, IOException {
        System.out.println("Test");
        String document = "<ns0:document xmlns:ns6=\"http://schemas.ogf.org/nsi/2013/12/services/definition\" \n" +
"        xmlns:ns5=\"http://nordu.net/namespaces/2013/12/gnsbod\" \n" +
"        xmlns:ns7=\"http://schemas.ogf.org/nsi/2013/09/topology#\" \n" +
"        xmlns:ns0=\"http://schemas.ogf.org/nsi/2014/02/discovery/types\" \n" +
"        xmlns:ns2=\"http://schemas.es.net/nsi/2014/03/dds/configuration\" \n" +
"        xmlns:ns1=\"http://schemas.ogf.org/nml/2013/05/base#\" \n" +
"        xmlns:ns4=\"http://schemas.ogf.org/nsi/2014/02/discovery/nsa\" \n" +
"        xmlns:ns3=\"urn:ietf:params:xml:ns:vcard-4.0\"\n" +
"    id=\"urn:ogf:network:icair.org:2013:nsa\" href=\"https://nsi-aggr-west.es.net/discovery/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.nsa.v1%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa\" version=\"2015-12-08T09:01:03Z\" expires=\"2016-12-07T01:07:46.863-08:00\">\n" +
"    <nsa>urn:ogf:network:icair.org:2013:nsa</nsa>\n" +
"    <type>vnd.ogf.nsi.nsa.v1+xml</type>\n" +
"    <content>\n" +
"        <ns4:nsa id=\"urn:ogf:network:icair.org:2013:nsa\" version=\"2015-12-08T09:01:03Z\">\n" +
"            <name>icair.org</name>\n" +
"            <softwareVersion>OpenNSA-git-20151208</softwareVersion>\n" +
"            <startTime>2015-12-08T09:01:03Z</startTime>\n" +
"            <networkId>urn:ogf:network:icair.org:2013:topology</networkId>\n" +
"            <interface>\n" +
"                <type>application/vnd.ogf.nsi.cs.v2.provider+soap</type>\n" +
"                <href>\n" +
"                    https://pmri061.it.northwestern.edu:9443/NSI/services/CS2\n" +
"                </href>\n" +
"            </interface>\n" +
"            <interface>\n" +
"                <type>application/vnd.org.ogf.nsi.cs.v2+soap</type>\n" +
"                <href>\n" +
"                    https://pmri061.it.northwestern.edu:9443/NSI/services/CS2\n" +
"                </href>\n" +
"            </interface>\n" +
"            <interface>\n" +
"                <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"                <href>\n" +
"                    https://pmri061.it.northwestern.edu:9443/NSI/icair.org:2013.nml.xml\n" +
"                </href>\n" +
"            </interface>\n" +
"            <feature type=\"vnd.ogf.nsi.cs.v2.role.aggregator\"/>\n" +
"            <feature type=\"vnd.ogf.nsi.cs.v2.role.uPA\"/>\n" +
"            <peersWith>urn:ogf:network:netherlight.net:2013:nsa:safnari</peersWith>\n" +
"            <peersWith>urn:ogf:network:ampath.net:2013:nsa</peersWith>\n" +
"        </ns4:nsa>\n" +
"    </content>\n" +
"</ns0:document>";

        DocumentType doc = DdsParser.getInstance().xml2Jaxb(DocumentType.class, document);
        Document decoded = Decoder.decode(doc.getContent().getContentTransferEncoding(), doc.getContent().getContentType(), doc.getContent().getValue());
    }


    @Test
    public void decodeDdsCollectionTest() throws JAXBException, IOException {
        CollectionType collection = DdsParser.getInstance().readCollection("src/test/resources/config/testDocuments.xml");
        assertNotNull(collection);
        for (DocumentType doc : collection.getDocuments().getDocument()) {
            Document decoded = Decoder.decode(doc.getContent().getContentTransferEncoding(), doc.getContent().getContentType(), doc.getContent().getValue());
            assertNotNull(decoded);
            if (NsiConstants.NSI_DOC_TYPE_NSA_V1.equalsIgnoreCase(doc.getType())) {
                assertEquals(doc.getId(), NsaParser.getInstance().dom2Nsa(decoded).getId());
            }
            else if (NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2.equalsIgnoreCase(doc.getType())) {
                assertEquals(doc.getId(), NmlParser.getInstance().dom2Nml(decoded).getId());
            }
            else {
                fail();
            }
        }
    }
}
