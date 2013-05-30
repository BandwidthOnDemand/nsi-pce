package net.es.nsi.pce.svc;


import net.es.nsi.pce.config.http.HttpConfig;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;


public class AggServer {
    private org.apache.cxf.endpoint.Server server;

    private static AggServer instance;

    public static AggServer getInstance(String url, String configFile) throws Exception {
        if (instance == null) {
            instance = new AggServer(url, configFile);
        }
        return instance;
    }
    public static AggServer getInstance(HttpConfig conf) throws Exception {
        if (instance == null) {
            instance = new AggServer(conf.url, conf.bus);
        }
        return instance;
    }

    private AggServer(String url, String configFile) throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus(configFile);
        BusFactory.setDefaultBus(bus);

        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(AggServiceImpl.class);
        sf.setResourceProvider(AggServiceImpl.class,
                new SingletonResourceProvider(new AggServiceImpl()));
        sf.setAddress(url);
        server = sf.create();
    }

    public void stop() {
        server.stop();
        server.destroy();

    }










}