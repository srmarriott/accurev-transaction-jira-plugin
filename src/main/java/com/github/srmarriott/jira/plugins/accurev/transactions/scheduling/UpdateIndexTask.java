package com.github.srmarriott.jira.plugins.accurev.transactions.scheduling;

import com.atlassian.sal.api.scheduling.PluginJob;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.transactions.TransactionIndexer;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UpdateIndexTask implements PluginJob {
    private final static Logger logger = LoggerFactory.getLogger(UpdateIndexTask.class);

    @Override
    public void execute(Map<String, Object> jobDataMap) {
        final UpdateIndexMonitorImpl monitor = (UpdateIndexMonitorImpl) jobDataMap.get("UpdateIndexMonitorImpl:instance");
        final MultipleAccuRevDepotManager multipleAccuRevDepotManager = (MultipleAccuRevDepotManager) jobDataMap.get("MultipleAccuRevDepotManager");
        assert monitor != null;
        try {
            if (multipleAccuRevDepotManager == null) {
                return; // Just return --- the plugin is disabled. Don't log anything.
            }

            TransactionIndexer transactionIndexer = multipleAccuRevDepotManager.getTransactionIndexer();
            if (transactionIndexer != null) {	
                transactionIndexer.updateIndex();
            }
            else {
                logger.warn("Tried to index changes but MultipleAccuRevManager has no transaction indexer?");
            }
        } catch (Exception e) {
            logger.error("Error indexing changes: " + e);
        }

    }
}