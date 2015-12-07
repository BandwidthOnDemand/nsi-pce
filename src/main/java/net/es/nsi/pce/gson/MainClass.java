/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.gson;

import java.util.ArrayList;
import java.util.List;
import net.es.nsi.pce.jaxb.topology.NsaType;

/**
 *
 * @author hacksaw
 */
public class MainClass {

  public static void main(String[] args) throws Exception {
      /*
    String keystoreFilename = "my.keystore";

    char[] password = "password".toCharArray();
    String alias = "alias";

    FileInputStream fIn = new FileInputStream(keystoreFilename);
    KeyStore keystore = KeyStore.getInstance("JKS");

    keystore.load(fIn, password);

    Certificate cert = keystore.getCertificate(alias);

    System.out.println(cert);
    * */

    List<NsaType> list = new ArrayList<>();
    NsaType nsa = new NsaType();
    nsa.setId("NSA A");
    nsa.setHref("http://google.com/a");
    list.add(nsa);
    nsa = new NsaType();
    nsa.setId("NSA B");
    nsa.setHref("http://google.com/b");
    list.add(nsa);

    JsonProxy proxy = new JsonProxy();
    String json;
    json = proxy.serializeList(list, NsaType.class);
    System.out.println(json);
    for (NsaType nsa1 : proxy.deserializeList(json, NsaType.class)) {
        System.out.println(nsa1.getId());
    }
  }
}
