package net.es.nsi.pce.common;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

public class PCEServer {
    private org.apache.cxf.endpoint.Server server;

    private static PCEServer instance;

    public static PCEServer getInstance(String url, String configFile) throws Exception {
        if (instance == null) {
            instance = new PCEServer(url, configFile);
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