/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.messages;

import java.io.Serializable;
import java.util.Date;
import net.es.nsi.pce.discovery.jaxb.SubscriptionType;

/**
 *
 * @author hacksaw
 */
public class RemoteSubscription implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ddsURL;
    private Date lastModified = new Date(0);
    private SubscriptionType subscription;

    /**
     * @return the ddsURL
     */
    public String getDdsURL() {
        return ddsURL;
    }

    /**
     * @param ddsURL the ddsURL to set
     */
    public void setDdsURL(String ddsURL) {
        this.ddsURL = ddsURL;
    }

    /**
     * @return the subscription
     */
    public SubscriptionType getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(SubscriptionType subscription) {
        this.subscription = subscription;
    }

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

}
