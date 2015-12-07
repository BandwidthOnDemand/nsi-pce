package net.es.nsi.pce.sched;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import net.es.nsi.pce.jaxb.management.TimerStatusType;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.quartz.Job;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import static org.quartz.JobKey.jobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import static org.quartz.TriggerBuilder.newTrigger;
import org.quartz.TriggerKey;
import static org.quartz.TriggerKey.triggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a simple scheduler for timer related tasks using the Quartz
 * scheduling package.
 *
 * @author hacksaw
 */
public class PCEScheduler {
    // Logging facility.
    private final Logger log = LoggerFactory.getLogger(getClass());

    // The quartz scheduler.
    private Scheduler scheduler = null;

    // Our registered scheduler items.
    private final Map<String, SchedulerItem> schedulerItems = new ConcurrentHashMap<>();

    /**
     * Private constructor loads the scheduler once and prevents
     * instantiation from other classes.
     */
    public void init() {
        try {
            Properties props = new Properties();
            props.setProperty(StdSchedulerFactory.PROP_SCHED_SKIP_UPDATE_CHECK, "true");
            props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
            props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            props.setProperty("org.quartz.threadPool.threadCount", "2");
            SchedulerFactory schedFact = new StdSchedulerFactory(props);
            this.scheduler = schedFact.getScheduler();
        } catch (SchedulerException ex) {
            log.error("PCEScheduler: failed to create", ex);
        }
    }

     /**
     * Returns an instance of this singleton class.
     *
     * @return The single PCEScheduler object instance.
     */
    public static PCEScheduler getInstance() {
        return SpringApplicationContext.getBean("pceScheduler", PCEScheduler.class);
    }
    /**
     * Builds a trigger key using the provided job name and group.
     *
     * @param jobName The job name of the timer item.
     * @param jobGroup The job group of the timer item.
     * @return A trigger key for use in Quartz.
     */
    private TriggerKey pceTriggerKey(String jobName, String jobGroup) {
        return triggerKey("Trigger-" + jobName, "Trigger-" + jobGroup);
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
     * Get the scheduler item associated with the supplied identifier.
     *
     * @param id The identifier of the scheduler item to retrieve.
     * @return The matching scheduler item.
     */
    public SchedulerItem get(String id) {
        return schedulerItems.get(id);
    }

    /**
     * Add the specified job to the scheduler with the first instance running
     * at current time + interval.
     *
     * @param jobName The name of the job.
     * @param jobGroup The name of the job group.
     * @param jobClass An instance of this class will be executed.
     * @param interval The time in milliseconds between invocations of this job.
     * @return The identifier of the newly created scheduler item.
     * @throws SchedulerException Is there is an issue scheduling the new job.
     */
    public String add(String jobName, String jobGroup, Class<? extends Job> jobClass, long interval) throws SchedulerException {
        String id = jobName + ":" + jobGroup;
        SchedulerItem item = schedulerItems.get(id);
        if (item != null) {
            throw new SchedulerException("Scheduler job (" + id + ") already exists in scheduler.");
        }

        // Make sure the requested job is not in the scheduler.
        if (scheduler.checkExists(jobKey(jobName, jobGroup))) {
            throw new SchedulerException("Scheduler add request failed, (" + id + ") already exist.");
        }

        // Add the job to the scheduler but have first instance start in "interval" time.
        Date currentDate = new Date(System.currentTimeMillis() + interval);
        Trigger trigger = newTrigger()
                .withIdentity(pceTriggerKey(jobName, jobGroup))
                .startAt(currentDate)
                .withSchedule(simpleSchedule()
                    .withIntervalInMilliseconds(interval)
                    .withRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY))
                .build();

        JobDetail jobDetail = newJob(jobClass)
                .withIdentity(jobKey(jobName, jobGroup))
                .storeDurably(true)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        item = new SchedulerItem(jobName, jobGroup, jobClass, interval);
        schedulerItems.put(item.getId(), item);
        return item.getId();
    }

    /**
     * Remove the specified job and associated schedule.
     *
     * @param id The identifier of the scheduler item to remove.
     * @return Returns true if the job was unscheduled, false otherwise.
     * @throws SchedulerException If an error occurs during remove schedule operation.
     * @throws NotFoundException The specified timer identifier was not found.
     */
    public boolean remove(String id) throws SchedulerException, NotFoundException {
        SchedulerItem item = schedulerItems.remove(id);
        if (item == null) {
            throw new NotFoundException("Job (" + id + ") does not exists in scheduler.");
        }

        return scheduler.deleteJob(jobKey(item.getJobName(), item.getJobGroup()));
    }

    /**
     * List the scheduled jobs.
     *
     * @return Collection of scheduled items.
     */
    public Collection<SchedulerItem> list() {
        return schedulerItems.values();
    }

    /**
     * Get the time of the next execution of the specified job.
     *
     * @param id The identifier of the schedule item to query for next run time.
     * @return Date of the next run time.
     * @throws SchedulerException If there is an issue getting the run time.
     * @throws NotFoundException If there is no matching scheduler entry.
     */
    public Date getNextRun(String id) throws SchedulerException, NotFoundException {
        SchedulerItem item = schedulerItems.get(id);
        if (item == null) {
            throw new NotFoundException("Job (" + id + ") does not exists in scheduler.");
        }

        // Make sure we have a trigger for the schedule item.
        Trigger oldTrigger = scheduler.getTrigger(pceTriggerKey(item.getJobName(), item.getJobGroup()));
        if (oldTrigger == null) {
            return null;
        }

        return oldTrigger.getNextFireTime();
    }

    /**
     * Get the status of the specified job schedule.
     *
     * @param id Identifier of the job to retrieve status.
     * @return Job status.
     * @throws SchedulerException If there is an issue getting job schedule.
     * @throws NotFoundException The specified timer identifier was not found.
     */
    public TimerStatusType getCurrentStatus(String id) throws SchedulerException, NotFoundException {
        SchedulerItem item = schedulerItems.get(id);
        if (item == null) {
            throw new NotFoundException("Job (" + id + ") does not exists in scheduler.");
        }

        // Make sure the requested job is in the scheduler.  Not sure if this
        // is a valid situation, but there may be timing issues.
        if (!scheduler.checkExists(jobKey(item.getJobName(), item.getJobGroup()))) {
            return TimerStatusType.UNKNOWN;
        }

        // Nothing we can do if the scheduler itself is haulted or not running.
        if (scheduler.isInStandbyMode() || scheduler.isShutdown()) {
            log.error("getCurrentStatus: scheduler not running");
            return TimerStatusType.HAULTED;
        }

        // Check the list of currently running jobs to see if this one is
        // running.  We need to cover the case where the schedule has no
        // registered triggers and was manually invoked.
        List<JobExecutionContext> currentJobs = scheduler.getCurrentlyExecutingJobs();
        for (JobExecutionContext jobCtx: currentJobs){
            String name = jobCtx.getJobDetail().getKey().getName();
            String group = jobCtx.getJobDetail().getKey().getGroup();
            if (name.equalsIgnoreCase(item.getJobName()) && group.equalsIgnoreCase(item.getJobGroup())) {
                return TimerStatusType.RUNNING;
            }
        }

        // Make sure we have a trigger for the schedule item.
        if (!scheduler.checkExists(pceTriggerKey(item.getJobName(), item.getJobGroup()))) {
            return TimerStatusType.HAULTED;
        }

        return TimerStatusType.SCHEDULED;
    }

    /**
     * Updates the recurring interval of a scheduled job.  The next execution
     * time is not modified and will remain the next trigger event.  Every
     * execution after that will occur at the new time.
     *
     * @param id Identifier of the job to update with the new internal value.
     * @param interval New interval value.
     * @return true if the interval was updated, false otherwise.
     * @throws SchedulerException If there was an issue updating the job schedule.
     * @throws NotFoundException The specified timer identifier was not found.
     */
    public boolean update(String id, long interval) throws SchedulerException, NotFoundException {
        SchedulerItem item = schedulerItems.get(id);
        if (item == null) {
            throw new NotFoundException("Scheduler job (" + id + ") does not exists in scheduler.");
        }

        item.setInterval(interval);

        // Get a handle to the old trigger.
        Trigger oldTrigger = scheduler.getTrigger(pceTriggerKey(item.getJobName(), item.getJobGroup()));

        // If there is no trigger then the job maybe haulted.  The new interval
        // will be set the next time we schedule a trigger.
        if (oldTrigger == null) {
            return true;
        }

        // Get a new trigger builder based on the old trigger.
        TriggerBuilder tb = oldTrigger.getTriggerBuilder();
        @SuppressWarnings("unchecked")
        Trigger newTrigger = tb.withSchedule(simpleSchedule()
                .withIntervalInMilliseconds(interval)
                .withRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY))
                .build();
        return (scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger) == null) ? false : true;
    }

    /**
     * Schedule the timer associated with the supplied identifier using the
     * stored schedule criteria.
     *
     * @param id Identifier of the timer.
     * @throws SchedulerException There was an error scheduling the timer,
     * @throws NotFoundException The specified timer identifier was not found.
     * @throws BadRequestException The timer already has an active schedule.
     */
    public void schedule(String id) throws SchedulerException, NotFoundException, BadRequestException {
        SchedulerItem item = schedulerItems.get(id);
        if (item == null) {
            throw new NotFoundException("Job (" + id + ") does not exists in scheduler.");
        }

        // Make sure the requested job is in the scheduler.
        if (!scheduler.checkExists(jobKey(item.getJobName(), item.getJobGroup()))) {
            throw new NotFoundException("Schedule request failed, (" + id + ") does not exist.");
        }

        // Make sure there is no existing trigger for the job.
        if (scheduler.checkExists(pceTriggerKey(item.getJobName(), item.getJobGroup()))) {
            throw new BadRequestException("Schedule request failed, (" + id + ") already scheduled.");
        }

        // Add the job to the scheduler but have first instance start in "interval" time.
        Date currentDate = new Date(System.currentTimeMillis() + item.getInterval());
        Trigger trigger = newTrigger()
                .forJob(jobKey(item.getJobName(), item.getJobGroup()))
                .withIdentity(pceTriggerKey(item.getJobName(), item.getJobGroup()))
                .startAt(currentDate)
                .withSchedule(simpleSchedule()
                    .withIntervalInMilliseconds(item.getInterval())
                    .withRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY))
                .build();

        scheduler.scheduleJob(trigger);
    }

    /**
     * Removes the schedule trigger associated with a job while leaving the job
     * in place.
     *
     * @param id The id of the scheduled item.
     * @return true if the trigger was removed, otherwise false.
     * @throws SchedulerException If there was an internal error.
     * @throws NotFoundException  The specified timer identifier was not found.
     */
    public boolean hault(String id) throws SchedulerException, NotFoundException {
        SchedulerItem item = schedulerItems.get(id);
        if (item == null) {
            throw new NotFoundException("Job (" + id + ") does not exists in scheduler.");
        }

        // Make sure we have a trigger for the schedule item.
        return scheduler.unscheduleJob(pceTriggerKey(item.getJobName(), item.getJobGroup()));
    }

    /**
     * Execute the scheduled job now.
     *
     * @param id Identifier of the job to execute now.
     * @throws SchedulerException If there was an issue executing the job schedule.
     * @throws NotFoundException The specified timer identifier was not found.
     */
    public void runNow(String id) throws SchedulerException, NotFoundException {
        SchedulerItem item = schedulerItems.get(id);
        if (item == null) {
            throw new NotFoundException("Job (" + id + ") does not exists in scheduler.");
        }

        // Make sure the requested job is in the scheduler.
        JobDetail job = scheduler.getJobDetail(jobKey(item.getJobName(), item.getJobGroup()));
        if (job == null){
            throw new NotFoundException("runNow request failed, (" + id + ") does not exist.");
        }

        // Check the list of currently running jobs for the one we are
        // interesting in running.
        List<JobExecutionContext> currentJobs = scheduler.getCurrentlyExecutingJobs();
        for (JobExecutionContext jobCtx: currentJobs){
            String name = jobCtx.getJobDetail().getKey().getName();
            String group = jobCtx.getJobDetail().getKey().getGroup();
            if (name.equalsIgnoreCase(item.getJobName()) && group.equalsIgnoreCase(item.getJobGroup())) {
                // We found the job already running so ignore this request.
                return;
            }
        }

        // Job is in the scheduler and it is not running so start it.
        scheduler.triggerJob(jobKey(item.getJobName(), item.getJobGroup()));
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

        scheduler.clear();
        scheduler.shutdown();
    }
}
