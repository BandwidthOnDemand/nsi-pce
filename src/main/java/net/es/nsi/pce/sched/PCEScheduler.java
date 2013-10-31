package net.es.nsi.pce.sched;


import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEScheduler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private Scheduler scheduler = null;
    
    /**
     * Private constructor loads the scheduler once and prevents
     * instantiation from other classes.
     */
    private PCEScheduler() {
        try {
            SchedulerFactory schedFact = new StdSchedulerFactory();
            this.scheduler = schedFact.getScheduler();
        } catch (SchedulerException ex) {
            log.error("PCEScheduler: failed to create", ex);
        }
    }
    
    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class PCESchedulerHolder {
        public static final PCEScheduler INSTANCE = new PCEScheduler();
    }    
    
     /**
     * Returns an instance of this singleton class.
     * 
     * @return The single PCEScheduler object instance.
     */
    public static PCEScheduler getInstance() {
            return PCESchedulerHolder.INSTANCE;
    }   
    
    public void start() throws SchedulerException {
        if (scheduler == null) {
            throw new SchedulerException("Failed to create scheduler");
        }
        
        scheduler.start();
    }
    
    public void add(String jobName, Class<?> jobClass, long interval) throws SchedulerException {
        // reload config from files every 10 seconds
        SimpleTrigger cfgReloadTrigger = new SimpleTrigger(jobName + "Trigger", jobName);
        cfgReloadTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        cfgReloadTrigger.setRepeatInterval(interval);
        JobDetail cfgReloadJobDetail = new JobDetail(jobName, jobName, jobClass);
        this.scheduler.scheduleJob(cfgReloadJobDetail, cfgReloadTrigger);
    }
    
    public void stop() throws SchedulerException {
        if (scheduler == null) {
            throw new SchedulerException("Failed to create scheduler");
        }

        scheduler.shutdown();
    }
}
