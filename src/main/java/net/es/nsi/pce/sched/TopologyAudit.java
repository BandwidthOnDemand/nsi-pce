package net.es.nsi.pce.sched;


import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyAudit implements Job {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        SpringContext sc = SpringContext.getInstance();

        log.info("TopologyAudit: running...");
        try {
            TopologyProvider tp = (TopologyProvider) sc.getContext().getBean("topologyProvider");
            tp.loadTopology();
        } catch (Exception ex) {
            log.error("TopologyAudit: failed to audit topology", ex);
        }
        log.info("TopologyAudit: completed!");
    }
}
