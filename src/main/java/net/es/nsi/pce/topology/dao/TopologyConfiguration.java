package net.es.nsi.pce.topology.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.config.jaxb.TopologyConfigurationType;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.schema.TopologyConfigurationParser;
import net.es.nsi.pce.spring.SpringApplicationContext;

/**
 *
 * @author hacksaw
 */
public class TopologyConfiguration {
    private final PceLogger pceLogger = PceLogger.getLogger();

    private String filename = null;

    private long lastModified = 0;

    private String baseURL = null;

    // Document Discovery Service endpoint.
    private String ddsURL = null;

    // Time between topology refreshes.
    private long auditInterval = 30*60*1000;  // Default 30 minute polling time.

    // Time of last audit.
    private long lastAudit = 0;

    // Default serviceType provided by topology.
    private String defaultServiceType = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public static TopologyConfiguration getInstance() {
        TopologyConfiguration configurationReader = SpringApplicationContext.getBean("topologyConfiguration", TopologyConfiguration.class);
        return configurationReader;
    }

    public synchronized void load() throws IllegalArgumentException, JAXBException, IOException, NullPointerException {
        // Make sure the condifuration file is set.
        if (getFilename() == null || getFilename().isEmpty()) {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw new IllegalArgumentException();
        }

        File file = null;
        try {
            file = new File(getFilename());
        }
        catch (NullPointerException ex) {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw ex;
        }

        long lastMod = file.lastModified();

        // If file was not modified since out last load then return.
        if (lastMod <= lastModified) {
            return;
        }

        TopologyConfigurationType config;

        try {
            config = TopologyConfigurationParser.getInstance().parse(getFilename());
        }
        catch (FileNotFoundException nf) {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw nf;
        }
        catch (JAXBException | IOException jaxb) {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_INVALID_XML, "filename", getFilename());
            throw jaxb;
        }

        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_MISSING_BASE_URL, "baseURL");
            throw new FileNotFoundException(PceErrors.CONFIGURATION_MISSING_BASE_URL.getDescription());
        }

        setBaseURL(config.getBaseURL());

        if (config.getDdsURL() == null || config.getDdsURL().isEmpty()) {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_MISSING_DDS_URL, "ddsURL");
            throw new FileNotFoundException(PceErrors.CONFIGURATION_MISSING_DDS_URL.getDescription());
        }

        setDdsURL(config.getDdsURL());

        if (config.getAuditInterval() > 0) {
            setAuditInterval(config.getAuditInterval());
        }
        else {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_INVALID_AUDIT_INTERVAL, "auditInterval", Long.toString(config.getAuditInterval()));
        }

        if (config.getDefaultServiceType() != null &&
                !config.getDefaultServiceType().isEmpty()) {
            setDefaultServiceType(config.getDefaultServiceType());
        }
        else {
            pceLogger.errorAudit(PceErrors.CONFIGURATION_MISSING_SERVICETYPE, "defaultServiceType", getDefaultServiceType());
        }

        lastModified = lastMod;
    }

    /**
     * @return the location
     */
    public String getDdsURL() {
        return ddsURL;
    }

    /**
     * @param location the location to set
     */
    public void setDdsURL(String ddsURL) {
        this.ddsURL = ddsURL;
    }

    /**
     * @return the auditInterval
     */
    public long getAuditInterval() {
        return auditInterval;
    }

    /**
     * @param auditInterval the auditInterval to set
     */
    public void setAuditInterval(long auditInterval) {
        this.auditInterval = auditInterval;
    }

    /**
     * @return the lastAudit
     */
    public long getLastAudit() {
        return lastAudit;
    }

    /**
     * @param lastAudit the lastAudit to set
     */
    public void setLastAudit(long lastAudit) {
        this.lastAudit = lastAudit;
    }

    /**
     * @return the defaultServiceType
     */
    public String getDefaultServiceType() {
        return defaultServiceType;
    }

    /**
     * @param defaultServiceType the defaultServiceType to set
     */
    public void setDefaultServiceType(String defaultServiceType) {
        this.defaultServiceType = defaultServiceType;
    }

    /**
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * @param baseURL the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }
}
