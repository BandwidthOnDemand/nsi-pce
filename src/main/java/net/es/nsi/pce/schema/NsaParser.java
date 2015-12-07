package net.es.nsi.pce.schema;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.topology.NsaNsaType;
import net.es.nsi.pce.jaxb.topology.ObjectFactory;
import org.w3c.dom.Document;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NsaParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.pce.jaxb.topology";
    private static final ObjectFactory factory = new ObjectFactory();

    private NsaParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final NsaParser INSTANCE = new NsaParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static NsaParser getInstance() {
            return ParserHolder.INSTANCE;
    }

    public NsaNsaType readNsa(String filename) throws FileNotFoundException, JAXBException, IOException {
        return getInstance().parseFile(NsaNsaType.class, filename);
    }

    public void writeNsa(String file, NsaNsaType nsa) throws JAXBException, IOException {
        // Parse the specified file.
        JAXBElement<NsaNsaType> element = factory.createNsaNsa(nsa);
        getInstance().writeFile(element, file);
    }

    public NsaNsaType dom2Nsa(Document doc) throws JAXBException {
        JAXBElement<?> dom2Jaxb = getInstance().dom2Jaxb(doc);
        if (dom2Jaxb.getValue() instanceof NsaNsaType) {
            return NsaNsaType.class.cast(dom2Jaxb.getValue());
        }

        return null;
    }

    public NsaNsaType xml2Nsa(String xml) throws JAXBException {
        return getInstance().xml2Jaxb(NsaNsaType.class, xml);
    }
}
