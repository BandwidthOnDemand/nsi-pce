package net.es.nsi.pce.sched;


import net.es.nsi.pce.config.SpringContext;
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
    
    /**
     * This method is invoked by the scheduler when it is time for a topology
     * audit.
     * 
     * @param jobExecutionContext
     * @throws JobExecutionException 
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("TopologyAudit: running...");
        
        // Get a reference to the topology provider.
        SpringContext sc = SpringContext.getInstance();
        TopologyProvider tp = (TopologyProvider) sc.getContext().getBean("topologyProvider");
        
        // Invoke the topology audit.
        try {
            tp.loadTopology();
        } catch (Exception ex) {
            log.error("TopologyAudit: failed to audit topology", ex);
        }
        log.info("TopologyAudit: completed!");
    }
}
