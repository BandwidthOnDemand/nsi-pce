package net.es.nsi.pce.common;

import net.es.nsi.pce.config.JsonNsaConfigProvider;

public class Invoker {
    public static void main(String args[]) throws Exception {
        PCEServer.getInstance("http://jupiter.es.net:8400/", "config/jetty.xml");

        AggServer.getInstance("http://jupiter.es.net:8401/", "config/jetty.xml");


        System.out.print("Loading NSA config... ");
        JsonNsaConfigProvider prov = JsonNsaConfigProvider.getInstance();
        prov.loadConfig("config/nsa.json");
        System.out.println("done.");


        System.out.println("PCE running...");
        while (true) {
            Thread.sleep(1);
        }

    }
}
