package net.es.nsi.pce.svc;


import net.es.nsi.pce.config.http.HttpConfig;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

public class PCEServer {
    private org.apache.cxf.endpoint.Server server;

    private static PCEServer instance;

    public static PCEServer getInstance() {
        return instance;
    }

    public static PCEServer makeServer(HttpConfig conf) throws Exception {
        if (instance == null) {
            instance = new PCEServer(conf.url, conf.bus);
        }
        return instance;
    }


    private PCEServer(String url, String configFile) throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus(configFile);
        BusFactory.setDefaultBus(bus);


        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(FindPathServiceImpl.class);
        sf.setResourceProvider(FindPathServiceImpl.class,
                new SingletonResourceProvider(new FindPathServiceImpl()));
        sf.setAddress(url);
        server = sf.create();
    }

    public void stop() {
        server.stop();
        server.destroy();

    }


}