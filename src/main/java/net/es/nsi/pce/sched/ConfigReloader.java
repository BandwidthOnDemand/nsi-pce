package net.es.nsi.pce.sched;


import net.es.nsi.pce.config.JsonNsaConfigProvider;
import net.es.nsi.pce.topo.JsonTopoConfigProvider;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ConfigReloader implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // System.out.println("running config reloader");
        try {
            JsonTopoConfigProvider.getInstance().loadConfig();
            JsonNsaConfigProvider.getInstance().loadConfig();
        } catch (Exception ex) {
            //

        }

    }
}
