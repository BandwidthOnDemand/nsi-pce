package net.es.nsi.pce.sched;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class PCEScheduler {
    private static PCEScheduler instance = new PCEScheduler();
    private Scheduler scheduler;

    public static PCEScheduler getInstance() {
        return instance;
    }

    private PCEScheduler() {
    }
    public void start() throws Exception {
        SchedulerFactory schedFact = new StdSchedulerFactory();
        this.scheduler = schedFact.getScheduler();

        // reload config from files every second
        SimpleTrigger cfgReloadTrigger = new SimpleTrigger("ConfigReloaderTrigger", "ConfigReloader");
        cfgReloadTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        cfgReloadTrigger.setRepeatInterval(1000);
        JobDetail cfgReloadJobDetail = new JobDetail("ConfigReloader", "ConfigReloader", ConfigReloader.class);
        this.scheduler.scheduleJob(cfgReloadJobDetail, cfgReloadTrigger);

        scheduler.start();

    }
    public void stop() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

}
