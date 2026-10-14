package net.es.nsi.pce.util;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

/**
 * Decoding of DDS document content for every transfer encoding and content type the DDS may use.
 * Each case encodes the same document.
 */
@RunWith(Parameterized.class)
public class ContentDecodingTest {
    private static final String DOCUMENT = "<a b=\"c\">d=e é</a>\n";

    @Parameter(0)
    public String transferEncoding;
    @Parameter(1)
    public String contentType;
    @Parameter(2)
    public String encoded;

    @Parameters(name = "{0} {1}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {"base64", "text/plain", "PGEgYj0iYyI+ZD1lIMOpPC9hPgo="},
            {"base64", "application/xml", "PGEgYj0iYyI+ZD1lIMOpPC9hPgo="},
            {"base64", "application/x-gzip", "H4sIAAAAAAAA/7NJVEiyVUpWskuxTVU4vNJGP9GOCwASN15DFAAAAA=="},
            {"quoted-printable", "text/plain", "<a b=3D\"c\">d=3De =C3=A9</a>\n"},
            {"uuencode", "text/plain", "begin 644 x\n4/&$@8CTB8R(^9#UE(,.I/\"]A/@H \n`\nend\n"},
            {"8bit", "text/plain", DOCUMENT},
            {"binary", "text/plain", DOCUMENT},
            {"", "", DOCUMENT},
            {null, null, DOCUMENT},
        });
    }

    @Test
    public void decodesToBytes() throws Exception {
        byte[] decoded = Decoder.decode2ByteArray(transferEncoding, contentType, encoded);
        assertEquals(DOCUMENT, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    public void decodesToDom() throws Exception {
        Document dom = Decoder.decode2Dom(transferEncoding, contentType, encoded);
        assertEquals("a", dom.getDocumentElement().getTagName());
        assertEquals("c", dom.getDocumentElement().getAttribute("b"));
        assertEquals("d=e é", dom.getDocumentElement().getTextContent());
    }
}
