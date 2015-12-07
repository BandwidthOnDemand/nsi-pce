/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.pce.schema.DomParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author hacksaw
 */
public class Decoder {
    private final static Logger log = LoggerFactory.getLogger(Decoder.class);

    public static Document decode(String contentTransferEncoding,
            String contentType, String source) throws IOException, UnsupportedEncodingException, RuntimeException {
        try {
            InputStream cteis = ContentTransferEncoding.decode(contentTransferEncoding, source);
            InputStream ctis = ContentType.decode(contentType, cteis);
            return DomParser.xml2Dom(ctis);
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        } catch (ParserConfigurationException | SAXException ex) {
            log.error("decode: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }
}
