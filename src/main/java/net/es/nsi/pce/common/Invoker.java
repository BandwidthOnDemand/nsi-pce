package net.es.nsi.pce.common;

import net.es.nsi.pce.config.HttpConfig;
import net.es.nsi.pce.config.JsonHttpConfigProvider;
import net.es.nsi.pce.config.JsonNsaConfigProvider;

public class Invoker {
    public static void main(String args[]) throws Exception {
        JsonHttpConfigProvider htProv = JsonHttpConfigProvider.getInstance();
        System.out.print("Loading HTTP config... ");
        htProv.loadConfig("config/http.json");
        HttpConfig pceConf = htProv.getConfig("pce");
        HttpConfig aggConf = htProv.getConfig("agg");

        System.out.print("done. Starting HTTP servers...\n");
        PCEServer.getInstance(pceConf);
        AggServer.getInstance(aggConf);
        System.out.println("HTTP servers started.");


        System.out.print("Loading NSA config... ");
        JsonNsaConfigProvider nsaprov = JsonNsaConfigProvider.getInstance();
        nsaprov.loadConfig("config/nsa.json");
        System.out.println("done.");


        System.out.println("PCE running...");
        while (true) {
            Thread.sleep(1);
        }

    }
}
