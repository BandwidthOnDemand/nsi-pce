package net.es.nsi.pce.sched;


import net.es.nsi.pce.config.SpringContext;
import net.es.nsi.pce.pf.api.topo.TopologyProvider;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ConfigReloader implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        SpringContext sc = SpringContext.getInstance();

        // System.out.println("running config reloader");
        try {
            TopologyProvider tp = (TopologyProvider) sc.getContext().getBean("topologyProvider");
            tp.loadTopology();

        } catch (Exception ex) {
            //

        }

    }
}
