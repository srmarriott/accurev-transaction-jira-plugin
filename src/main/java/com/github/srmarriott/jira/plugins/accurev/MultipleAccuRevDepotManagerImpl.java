package com.github.srmarriott.jira.plugins.accurev;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.opensymphony.module.propertyset.PropertySet;

import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.atlassian.jira.security.PermissionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.core.exception.InfrastructureException;
import com.github.srmarriott.jira.plugins.accurev.transactions.TransactionIndexer;


public class MultipleAccuRevDepotManagerImpl implements MultipleAccuRevDepotManager {
	private final JiraPropertySetFactory jiraPropertySetFactory;
	private TransactionIndexer transactionIndexer;
	private final static Logger log = LoggerFactory.getLogger(MultipleAccuRevDepotManagerImpl.class);
	private PropertySet pluginProperties;
	
    public static final String APP_PROPERTY_PREFIX = "srmarriott.jira.plugins.accurev";
    public static final String DEPOT_PROPERTY = "srmarriott.jira.plugins.accurev.depot";
    public static final String LAST_DEPOT_ID = "last.depot.id";
    public static final long FIRST_DEPOT_ID = 1;
    
    private Map<Long, AccuRevDepotManager> managerMap = new HashMap<Long, AccuRevDepotManager>();
    
    private long lastDepotId;
	
    public MultipleAccuRevDepotManagerImpl(
            VersionManager versionManager,
            IssueManager issueManager,
            PermissionManager permissionManager,
            ChangeHistoryManager changeHistoryManager,
            JiraPropertySetFactory jiraPropertySetFactory,
            IndexPathManager indexPathManager) {
        this.jiraPropertySetFactory = jiraPropertySetFactory;
        managerMap = loadAccuRevManagers();
        // create transaction indexer once we know we have succeed initializing our depots
        this.transactionIndexer = new TransactionIndexer(this, versionManager, issueManager, permissionManager, changeHistoryManager, indexPathManager);
    }
    
    public TransactionIndexer getTransactionIndexer() {
        return transactionIndexer;
    }
    
    private Map<Long, AccuRevDepotManager> loadAccuRevManagers() {
        return loadManagersFromJiraProperties();
    }
    
    private Map<Long, AccuRevDepotManager> loadManagersFromJiraProperties() {
        pluginProperties = jiraPropertySetFactory.buildCachingDefaultPropertySet(APP_PROPERTY_PREFIX, true);
        lastDepotId = pluginProperties.getLong(LAST_DEPOT_ID);
        log.debug("LAST_DEPOT_ID: " + lastDepotId);

        Map<Long, AccuRevDepotManager> managers = new LinkedHashMap<Long, AccuRevDepotManager>();
        for (long i = FIRST_DEPOT_ID; i <= lastDepotId; i++) {
            AccuRevDepotManager mgr = createManagerFromPropertySet(i, jiraPropertySetFactory.buildCachingPropertySet(DEPOT_PROPERTY, i, true));
            if (mgr != null)
                managers.put(i, mgr);
            else
            	log.debug("null mgr created");
        }
        return managers;
    }
    
    private AccuRevDepotManager createManagerFromPropertySet(long index, PropertySet properties) {
        try {
            if (properties.getKeys().isEmpty()) {
            	log.debug("properties.getKey is empty!");
                return null;
            }
            return new AccuRevDepotManagerImpl(index, properties);
        }
        catch (IllegalArgumentException e) {
            log.error("Error creating AccuRevDepotManager " + index + ". Probably was missing a required field (e.g., depot name). Skipping it.", e);
            return null;
        }
    }
    
    public AccuRevDepotManager createDepot(AccuRevProperties properties)
    {
        long depotId;
        synchronized (this)
        {
            depotId = ++lastDepotId;
            pluginProperties.setLong(LAST_DEPOT_ID, lastDepotId);
        }

        PropertySet set = jiraPropertySetFactory.buildCachingPropertySet(DEPOT_PROPERTY, depotId, true);
        AccuRevDepotManager accuRevDepotManager = new AccuRevDepotManagerImpl(depotId, AccuRevProperties.Util.fillPropertySet(properties, set));

        managerMap.put(accuRevDepotManager.getId(), accuRevDepotManager);
        if (isIndexingRevisions())
        {
            transactionIndexer.addDepot(accuRevDepotManager);
        }

        return accuRevDepotManager;
    }
    
    public void removeDepot(long depotId) {
        AccuRevDepotManager original = managerMap.get(depotId);
        if (original == null) {
            return;
        }
        try {
            managerMap.remove(depotId);

            // Would like to just call remove() but this version doesn't appear to have that, remove all of it's properties instead
            //for (String key : new ArrayList<String>(original.getProperties().getKeys()))
            //    original.getProperties().remove(key); TODO
            original.getProperties().remove();

            if (transactionIndexer != null)
                transactionIndexer.removeEntries(original);
        }
        catch (Exception e) {
            throw new InfrastructureException("Could not remove depot index", e);
        }
    }
    
    public AccuRevDepotManager updateDepot(long depotId, AccuRevProperties properties) {
        AccuRevDepotManager accuRevDepotManager = getDepot(depotId);
        accuRevDepotManager.update(properties);
        return accuRevDepotManager;
    }
    
    public boolean isIndexingRevisions() {
        return transactionIndexer != null;
    }
    
    public Collection<AccuRevDepotManager> getDepotList() {
        return managerMap.values();
    }
    
    public AccuRevDepotManager getDepot(long depotId) {
        return managerMap.get(depotId);
    }
    
    public Map<Long, AccuRevDepotManager> getMap() {
    	return managerMap;
    }
    
    void startTransactionIndexer() {
        getTransactionIndexer().start();
    }
    
    public void start() {
        try {
            if (isIndexingRevisions()) {
                startTransactionIndexer();
            }
        }
        catch (InfrastructureException ie) {
            /* Log error, don't throw. Otherwise, we get SVN-234 */
            log.error("Error starting " + getClass(), ie);
        }
    }
}