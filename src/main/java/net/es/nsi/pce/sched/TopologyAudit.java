package net.es.nsi.pce.sched;


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the scheduler job for topology audit.
 *
 * @author hacksaw
 */
@DisallowConcurrentExecution
public class TopologyAudit implements Job {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String FULL_JOBNAME = "FullTopologyAudit";
    public static final String QUICK_JOBNAME = "QuickTopologyAudit";
    public static final String JOBGROUP = "TopologyManagement";

    /**
     * This method is invoked by the scheduler when it is time for a topology
     * audit.
     *
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        log.info("TopologyAudit: running " + jobExecutionContext.getJobDetail().getKey().getName()
            + ", " + memStats(memoryBean));

        // Invoke the topology audit.
        try {
            boolean success = ConfigurationManager.INSTANCE.getTopologyProvider().loadTopology();
            if (success) {
                log.info("TopologyAudit: successful, scheduling next iteration");
                scheduleFull(jobExecutionContext);
            }
            else {
                log.error("TopologyAudit: failed, scheduling early audit");
                scheduleQuick(jobExecutionContext);
            }
        }
        catch (OutOfMemoryError ex) {
            log.error("TopologyAudit: " + memStats(memoryBean), ex);
            System.exit(-1);
        }
        catch (Exception ex) {
            log.error("TopologyAudit: failed to audit topology", ex);
            try {
                scheduleQuick(jobExecutionContext);
            } catch (SchedulerException ex1) {
               log.error("TopologyAudit: failed to reschedule audit", ex1);
            }
        }

        log.info("TopologyAudit: completed - " + memStats(memoryBean));
    }

    private static final int MEGABYTE = (1024*1024);
    private String memStats(MemoryMXBean memoryBean) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long maxMemory = heapUsage.getMax() / MEGABYTE;
        long usedMemory = heapUsage.getUsed() / MEGABYTE;
        return "Memory use :" + usedMemory + "M/" + maxMemory + "M";
    }

    private void scheduleFull(JobExecutionContext jobExecutionContext) throws SchedulerException {
        JobKey key = jobExecutionContext.getJobDetail().getKey();
        if (JOBGROUP.equalsIgnoreCase(key.getGroup()) && FULL_JOBNAME.equalsIgnoreCase(key.getName())) {
            return;
        }

        // Unschedule the current schedule and add a Full audit timer.
        TopologyProvider topologyProvider = ConfigurationManager.INSTANCE.getTopologyProvider();
        long auditInterval = topologyProvider.getAuditInterval();
        PCEScheduler pceScheduler = (PCEScheduler) ConfigurationManager.INSTANCE.getApplicationContext().getBean("pceScheduler");
        pceScheduler.remove(key);
        pceScheduler.add(TopologyAudit.FULL_JOBNAME, TopologyAudit.JOBGROUP, TopologyAudit.class, auditInterval*1000);
    }

    private void scheduleQuick(JobExecutionContext jobExecutionContext) throws SchedulerException {
        JobKey key = jobExecutionContext.getJobDetail().getKey();
        if (JOBGROUP.equalsIgnoreCase(key.getGroup()) && QUICK_JOBNAME.equalsIgnoreCase(key.getName())) {
            return;
        }

        // Unschedule the current schedule and add a quick timer.
        PCEScheduler pceScheduler = (PCEScheduler) ConfigurationManager.INSTANCE.getApplicationContext().getBean("pceScheduler");
        pceScheduler.remove(key);
        pceScheduler.add(TopologyAudit.QUICK_JOBNAME, TopologyAudit.JOBGROUP, TopologyAudit.class, 2*60*1000);
    }
}
