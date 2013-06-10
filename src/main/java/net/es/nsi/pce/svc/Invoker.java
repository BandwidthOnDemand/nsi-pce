package net.es.nsi.pce.svc;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.config.http.HttpConfigProvider;
import net.es.nsi.pce.config.http.JsonHttpConfigProvider;
import net.es.nsi.pce.config.nsa.JsonNsaConfigProvider;
import net.es.nsi.pce.config.nsa.NsaConfigProvider;
import net.es.nsi.pce.config.nsa.auth.AuthProvider;
import net.es.nsi.pce.sched.PCEScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Invoker {
    public static void main(String args[]) throws Exception {
        SpringContext sc = SpringContext.getInstance();

        ApplicationContext context = sc.initContext("config/beans.xml");

        HttpConfigProvider htProv = (HttpConfigProvider) context.getBean("httpConfigProvider");


        System.out.print("Loading HTTP config... ");
        HttpConfig pceConf = htProv.getConfig("pce");
        HttpConfig aggConf = htProv.getConfig("agg");

        System.out.print("done. Starting HTTP servers...\n");
        PCEServer.getInstance(pceConf);
        AggServer.getInstance(aggConf);
        System.out.println("HTTP servers started.");


        System.out.print("Loading NSA config... ");
        NsaConfigProvider ncp = (NsaConfigProvider) context.getBean("nsaConfigProvider");
        System.out.println("done.");

        System.out.print("Starting task scheduler...");
        PCEScheduler.getInstance().start();
        System.out.println(" done.");

        System.out.println("PCE running.");



        while (true) {
            Thread.sleep(1);
        }

    }
}
