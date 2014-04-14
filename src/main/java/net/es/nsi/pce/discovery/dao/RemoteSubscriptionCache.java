package net.es.nsi.pce.discovery.dao;

import net.es.nsi.pce.discovery.messages.RemoteSubscription;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
public class RemoteSubscriptionCache {
    // In-memory subscription cache indexed by subscriptionId.
    private Map<String, RemoteSubscription> remoteSubscriptions = new ConcurrentHashMap<>();
    
    public static RemoteSubscriptionCache getInstance() {
        RemoteSubscriptionCache remoteSubscriptionCache = SpringApplicationContext.getBean("remoteSubscriptionCache", RemoteSubscriptionCache.class);
        return remoteSubscriptionCache;
    }
    
    public RemoteSubscription get(String url) {
        return remoteSubscriptions.get(url);
    }
    
    public RemoteSubscription add(RemoteSubscription subscription) {
        return remoteSubscriptions.put(subscription.getDdsURL(), subscription);
    }
    
    public RemoteSubscription remove(String url) {
        return remoteSubscriptions.remove(url);
    }
    
    public Collection<RemoteSubscription> values() {
        return remoteSubscriptions.values();
    }
    
    public Set<String> keySet() {
        return remoteSubscriptions.keySet();
    }
}
