/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author hacksaw
 */ 
public class Convert {
    public static void main(String[] args) throws Exception {
        try(PrintWriter out = new PrintWriter("/Users/hacksaw/Desktop/eroTest-new.xml");BufferedReader br = new BufferedReader(new FileReader("/Users/hacksaw/Desktop/eroTest.xml"))) {
            boolean closed = true;
            StringBuilder sb = new StringBuilder();
            for(String line; (line = br.readLine()) != null; ) {
                if (line.contains("<content contentType=\"application/x-gzip\" contentTransferEncoding=\"base64\">")) {
                    closed = false;
                    out.println(line);
                    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
                }
                else if (line.contains("</content>")) {
                    closed = true;
                    byte[] compressed = compress(sb.toString().getBytes(Charset.forName("UTF-8")));
                    String encoded = Base64.getEncoder().encodeToString(compressed);
                    out.print(encoded);
                    sb.setLength(0);
                    out.println(line.trim());
                }
                else if (!closed) {
                    sb.append(line.trim()).append("\n");
                }
                else {
                    out.println(line);
                }
            }
        }
    }

    private static byte[] compress(byte[] source) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(source.length)) {
            return gzip(os, source).toByteArray();
        }
        catch (IOException io) {
            System.err.println(io.getLocalizedMessage());
            throw io;
        }
    }

    private static ByteArrayOutputStream gzip(ByteArrayOutputStream os, byte[] source) throws IOException {
        try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
            gos.write(source);
            return os;
        }
        catch (IOException io) {
            System.err.println("Failed to gzip source stream - " + io.getLocalizedMessage());
            throw io;
        }
    }
}
