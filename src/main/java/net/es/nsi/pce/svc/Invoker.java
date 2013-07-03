package net.es.nsi.pce.svc;

import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.config.http.HttpConfigProvider;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.sched.PCEScheduler;
import org.springframework.context.ApplicationContext;

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


        System.out.print("Loading SI config... ");
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
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
