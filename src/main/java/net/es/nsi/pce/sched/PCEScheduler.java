package net.es.nsi.pce.sched;


import java.util.Date;
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
    
    /**
     * Start the scheduler.
     * 
     * @throws SchedulerException If the scheduler has not been properly initialized.
     */
    public void start() throws SchedulerException {
        if (scheduler == null) {
            throw new SchedulerException("Failed to create scheduler");
        }
        
        scheduler.start();
    }
    
    /**
     * Add the specified job to the scheduler with the first instance running
     * as current time + interval.
     * 
     * @param jobName The name of the job.
     * @param jobClass An instance of this class will be executed.
     * @param interval The time in milliseconds between invocations of this job.
     * @throws SchedulerException 
     */
    public void add(String jobName, Class<?> jobClass, long interval) throws SchedulerException {
        // At the job to the scheduler but have first instance start in "interval" time.
        Date currentDate = new Date(System.currentTimeMillis() + interval);
        SimpleTrigger trigger = new SimpleTrigger(jobName + "Trigger", jobName);
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        trigger.setRepeatInterval(interval);
        trigger.setStartTime(currentDate);
        JobDetail cfgReloadJobDetail = new JobDetail(jobName, jobName, jobClass);
        this.scheduler.scheduleJob(cfgReloadJobDetail, trigger);
    }
    
    /**
     * Stop the scheduler.
     * 
     * @throws SchedulerException If there issues stopping the scheduler.
     */
    public void stop() throws SchedulerException {
        if (scheduler == null) {
            throw new SchedulerException("Failed to create scheduler");
        }

        scheduler.shutdown();
    }
}
