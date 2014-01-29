/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.sched;

import org.quartz.Job;

/**
 *
 * @author hacksaw
 */
public class SchedulerItem {
    private String id;
    private String jobName;
    private String jobGroup;
    private Class<? extends Job> jobClass;
    private long interval;

    public SchedulerItem() {}
    
    public SchedulerItem(String jobName, String jobGroup, Class<? extends Job> jobClass, long interval) {
        this.id = jobName + ":" + jobGroup;
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.jobClass = jobClass;
        this.interval = interval;
    }
    
    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName the jobName to set
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * @return the jobGroup
     */
    public String getJobGroup() {
        return jobGroup;
    }

    /**
     * @param jobGroup the jobGroup to set
     */
    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    /**
     * @return the jobClass
     */
    public Class<? extends Job> getJobClass() {
        return jobClass;
    }

    /**
     * @param jobClass the jobClass to set
     */
    public void setJobClass(Class<? extends Job> jobClass) {
        this.jobClass = jobClass;
    }

    /**
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
}
