/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.dao;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import org.junit.*;
import static org.junit.Assert.*;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.provider.Document;
import java.net.URLDecoder;

/**
 *
 * @author hacksaw
 */
public class TestDocumentCache {
    DiscoveryConfiguration config;

    @Before
    public void setUp() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException, IOException {
        config = new DiscoveryConfiguration();
        config.setFilename("src/test/resources/config/dds.xml");
        config.load();
        System.out.println("Configured nsaId=" + config.getNsaId());
    }

    @Test
    public void testLoadCache() throws FileNotFoundException, UnsupportedEncodingException {
        System.out.println("@Test - testLoadCache");
        DocumentCache cache = new DocumentCache(config);
        assertTrue(cache.isUseCache());
        assertTrue(cache.isUseDocuments());
        cache.load();
        Collection<Document> values = cache.values();
        assertTrue(values.size() > 0);
        for (Document document : values) {
            System.out.println("id=" + URLDecoder.decode(document.getId(), "UTF-8"));
        }
        int original = values.size();
        cache.expire();
        assertEquals(original, cache.values().size());
    }
}
