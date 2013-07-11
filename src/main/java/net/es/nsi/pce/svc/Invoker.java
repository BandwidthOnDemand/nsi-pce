package net.es.nsi.pce.svc;

import net.es.nsadb.PersistenceHolder;
import net.es.nsadb.auth.svc.AuthServer;
import net.es.nsadb.nsa.svc.NsaServer;
import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.config.http.HttpConfigProvider;
import net.es.nsi.pce.config.nsa.ServiceInfoProvider;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import net.es.nsi.pce.sched.PCEScheduler;
import org.springframework.context.ApplicationContext;

public class Invoker {
    private static boolean keepRunning = true;

    public static boolean isKeepRunning() {
        return keepRunning;
    }

    public static void setKeepRunning(boolean keepRunning) {
        Invoker.keepRunning = keepRunning;
    }

    public static void main(String args[]) throws Exception {
        SpringContext sc = SpringContext.getInstance();

        ApplicationContext context = sc.initContext("config/beans.xml");

        HttpConfigProvider htProv = (HttpConfigProvider) context.getBean("httpConfigProvider");


        System.out.print("Loading HTTP config... ");
        HttpConfig pceConf = htProv.getConfig("pce");
        HttpConfig aggConf = htProv.getConfig("agg");

        System.out.print("done. Starting HTTP servers...\n");
        PCEServer.makeServer(pceConf);
        AggServer.makeServer(aggConf);
        System.out.println("HTTP servers started.");


        System.out.print("Loading SI config... ");
        ServiceInfoProvider sip = (ServiceInfoProvider) context.getBean("serviceInfoProvider");
        System.out.println("done.");

        System.out.print("Loading topology... \n");
        TopologyProvider tp = (TopologyProvider) context.getBean("topologyProvider");
        tp.loadTopology();
        for (String networkId : tp.getNetworkIds()) {
            System.out.println("--- network: "+networkId);
        }
        System.out.println("done.");

        System.out.print("Starting task scheduler...");
        PCEScheduler.getInstance().start();
        System.out.println(" done.");

        System.out.println("PCE running.");

        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    public void run() {
                        System.out.println("Shutting down..");
                        PCEServer.getInstance().stop();
                        AggServer.getInstance().stop();
                        System.out.println("Shutdown complete.");
                        Invoker.setKeepRunning(false);
                    }
                }
        );

        while (keepRunning) {
            Thread.sleep(1);
        }



        while (true) {
            Thread.sleep(1);
        }

    }
}
