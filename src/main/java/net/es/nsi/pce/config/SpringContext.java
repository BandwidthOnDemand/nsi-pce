package net.es.nsi.pce.config;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringContext {
    private static SpringContext instance;
    public static SpringContext getInstance() {
        if (instance == null) {
            instance = new SpringContext();
        }
        return instance;
    }
    private SpringContext() {}

    private ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }
    public ApplicationContext initContext(String filename) {
        context = new FileSystemXmlApplicationContext(filename);
        return context;
    }

}
