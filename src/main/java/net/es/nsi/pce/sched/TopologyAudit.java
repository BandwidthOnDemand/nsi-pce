package net.es.nsi.pce.sched;


import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.spring.SpringContext;
import net.es.nsi.pce.topology.provider.TopologyProvider;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
    
    public static final String JOBNAME = "FullTopologyAudit";
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
        log.info("TopologyAudit: running " + jobExecutionContext.getJobDetail().getKey().getName());
        
        // Invoke the topology audit.
        try {
            ConfigurationManager.INSTANCE.getTopologyProvider().loadTopology();
        } catch (Exception ex) {
            log.error("TopologyAudit: failed to audit topology", ex);
        }
        log.info("TopologyAudit: completed!");
    }
}
