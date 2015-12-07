/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import java.io.FileNotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.topology.NmlTopologyType;
import net.es.nsi.pce.jaxb.topology.NsaNsaType;
import net.es.nsi.pce.schema.NmlParser;
import net.es.nsi.pce.schema.NsaParser;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class ParserTest {
    @Test
    public void testNsaSuccess() throws Exception {
        // This is a valid load of the file.
        NsaNsaType nsa = NsaParser.getInstance().readNsa("src/test/resources/nml/parse/tests/netherlight-nsa.xml");
        assertEquals("urn:ogf:network:netherlight.net:2013:nsa:safnari", nsa.getId());
    }

    @Test(expected=FileNotFoundException.class)
    public void testNsaFileNotFound() throws Exception {
        NsaParser.getInstance().readNsa("src/test/resources/nml/parse/tests/invalidfilename.xml");
    }

    @Test(expected=JAXBException.class)
    public void testNsaJAXBException() throws Exception {
        NsaParser.getInstance().readNsa("src/test/resources/nml/parse/tests/badcontent.xml");
    }

    @Test
    public void testNmlSuccess() throws Exception {
        // This is a valid load of the file.
        NmlTopologyType nml = NmlParser.getInstance().readTopology("src/test/resources/nml/parse/tests/ja.net-topology.xml");
        assertEquals("urn:ogf:network:ja.net:2013:topology", nml.getId());
    }

    @Test(expected=FileNotFoundException.class)
    public void testNmlFileNotFound() throws Exception {
        NmlParser.getInstance().readTopology("src/test/resources/nml/parse/tests/invalidfilename.xml");
    }

    @Test(expected=JAXBException.class)
    public void testNmlJAXBException() throws Exception {
        NmlParser.getInstance().readTopology("src/test/resources/nml/parse/tests/badcontent.xml");
    }
}
