/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.jaxb.DiscoveryConfigurationType;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;

/**
 *
 * @author hacksaw
 */
public class ConfigurationReader {
    private final PceLogger pceLogger = PceLogger.getLogger();
    
    public static final long MAX_AUDIT_INTERVAL = 86400000L; // 24 hours
    public static final long DEFAULT_AUDIT_INTERVAL = 1200000L; // 20 minutes
    public static final long MIN_AUDIT_INTERVAL = 300000L; // 5 mins
    
    public static final long MAX_EXPIRE_INTERVAL = 2592000000L; // 30 days
    public static final long DEFAULT_EXPIRE_INTERVAL = 86400000L; // 24 hours
    public static final long MIN_EXPIRE_INTERVAL = 3600000L; // 1 hour
    
    public static final int ACTORPOOL_MAX_SIZE = 100;
    public static final int ACTORPOOL_DEFAULT_SIZE = 20;
    public static final int ACTORPOOL_MIN_SIZE = 5;
    
    private String configuration = null;
    private long lastModified = 0;
    private String nsaId;
    private String baseURL;
    private String documents;
    private String cache;
    private long auditInterval = DEFAULT_AUDIT_INTERVAL;
    private long expiryInterval = DEFAULT_EXPIRE_INTERVAL;
    private int actorPool = ACTORPOOL_DEFAULT_SIZE;
    private Set<String> discoveryURL = new CopyOnWriteArraySet<>();
    
    public ConfigurationReader(String configuration) {
        this.configuration = configuration;
    }
    
    public synchronized void load() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException {
        // Make sure the condifuration file is set.
        if (configuration == null || configuration.isEmpty()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_FILENAME, "configurationFile", configuration);
            throw new IllegalArgumentException();
        }
        
        File file = null;
        try {
            file = new File(configuration);
        }
        catch (NullPointerException ex) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_FILENAME, "configurationFile", configuration);
            throw ex;
        }
        
        long lastMod = file.lastModified();
        
        // If file was not modified since out last load then return.
        if (lastMod <= lastModified) {
            return;
        }

        DiscoveryConfigurationType config;
        
        try {
            config = DiscoveryParser.getInstance().parse(configuration);
        }
        catch (FileNotFoundException nf) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_FILENAME, "configurationFile", getConfiguration());
            throw nf;
        }
        catch (JAXBException jaxb) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_XML, "configurationFile", getConfiguration());
            throw jaxb;
        }
        
        if (config.getNsaId() == null || config.getNsaId().isEmpty()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "nsaId", config.getNsaId());
            throw new FileNotFoundException("Invalid nsaId: " + config.getNsaId());
        }
        
        setNsaId(config.getNsaId());

        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "baseURL", config.getBaseURL());
            throw new FileNotFoundException("Invalid baseURL: " + config.getBaseURL());
        }
        
        setBaseURL(config.getBaseURL());

        if (config.getDocuments() == null || config.getDocuments().isEmpty()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "documents", config.getDocuments());
            throw new FileNotFoundException("Invalid document source directroy: " + config.getDocuments());
        }
        
        File dir = new File(config.getDocuments());
        if (!dir.exists()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "documents", config.getDocuments());
            throw new FileNotFoundException("Invalid document source directroy: " + config.getDocuments());            
        }

        setDocuments(config.getDocuments());
        
        if (config.getCache() == null || config.getCache().isEmpty()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "cache", config.getCache());
            throw new FileNotFoundException("Invalid cache location directory: " + config.getCache());
        }
        
        dir = new File(config.getCache());
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_CANNOT_CREATE_DIRECTORY, "cache", config.getCache());
                throw new FileNotFoundException("Cannot create directory: " + config.getCache());                
            }  
        }
        
        setCache(config.getCache());

        if (config.getAuditInterval() < MIN_AUDIT_INTERVAL || config.getAuditInterval() > MAX_AUDIT_INTERVAL) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "auditInterval", Long.toString(config.getAuditInterval()));
            setAuditInterval(DEFAULT_AUDIT_INTERVAL);
        }

        setAuditInterval(config.getAuditInterval());

        if (config.getExpiryInterval() < MIN_EXPIRE_INTERVAL || config.getExpiryInterval() > MAX_EXPIRE_INTERVAL) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "expiryInterval", Long.toString(config.getExpiryInterval()));
            setExpiryInterval(DEFAULT_EXPIRE_INTERVAL);
        }

        setExpiryInterval(config.getExpiryInterval());
        
        if (config.getActorPool() < ACTORPOOL_MIN_SIZE || config.getActorPool() > ACTORPOOL_MAX_SIZE) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "actorPool", Integer.toString(config.getActorPool()));
            setActorPool(ACTORPOOL_DEFAULT_SIZE);
        }

        setActorPool(config.getActorPool());
        
        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            pceLogger.errorAudit(PceErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "baseURL=" + config.getBaseURL());
            throw new FileNotFoundException("Invalid base URL: " + config.getBaseURL());
        }
        
        for (String url : config.getDiscoveryURL()) {
            discoveryURL.add(url);
        }
        
        lastModified = lastMod;
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
     * @return the expiryInterval
     */
    public long getExpiryInterval() {
        return expiryInterval;
    }

    /**
     * @param expiryInterval the expiryInterval to set
     */
    public void setExpiryInterval(long expiryInterval) {
        this.expiryInterval = expiryInterval;
    }

    /**
     * @return the discoveryURL
     */
    public Set<String> getDiscoveryURL() {
        return Collections.unmodifiableSet(discoveryURL);
    }

    /**
     * @param discoveryURL the discoveryURL to set
     */
    public void setDiscoveryURL(Set<String> discoveryURL) {
        this.discoveryURL = discoveryURL;
    }

    /**
     * @return the agentPool
     */
    public int getActorPool() {
        return actorPool;
    }

    /**
     * @param agentPool the agentPool to set
     */
    public void setActorPool(int agentPool) {
        this.actorPool = agentPool;
    }

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }

    /**
     * @return the documents
     */
    public String getDocuments() {
        return documents;
    }

    /**
     * @param documents the documents to set
     */
    public void setDocuments(String documents) {
        this.documents = documents;
    }

    /**
     * @return the cache
     */
    public String getCache() {
        return cache;
    }

    /**
     * @param cache the cache to set
     */
    public void setCache(String cache) {
        this.cache = cache;
    }

    /**
     * @return the configuration
     */
    public String getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
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
