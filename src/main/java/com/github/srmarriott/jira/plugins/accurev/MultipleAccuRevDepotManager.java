package com.github.srmarriott.jira.plugins.accurev;

import java.util.Collection;
import com.atlassian.jira.extension.Startable;
import com.github.srmarriott.jira.plugins.accurev.transactions.TransactionIndexer;

public interface MultipleAccuRevDepotManager extends Startable {
    String ACCUREV_SERVER = "accurev.server";
    String ACCUREV_PORT = "accurev.port";
    String ACCUREV_WEBLINK = "accurev.weblink";
    String ACCUREV_DEPOT_NAME = "accurev.depot.name";
    String ACCUREV_USERNAME = "accurev.username";
    String ACCUREV_PASSWORD = "accurev.password";
    String ACCUREV_TRANSACTION_INDEXING_KEY = "transaction.indexing";
    String ACCUREV_TRANSACTION_CACHE_SIZE_KEY = "transaction.cache.size";
    String ACCUREV_COMMENT_CACHE_SIZE_KEY = "comment.cache.size";
    long ACCUREV_MAX_TRANSACTION_UPDATE = 1500;
    
    boolean isIndexingRevisions();
    TransactionIndexer getTransactionIndexer();
    Collection<AccuRevDepotManager> getDepotList();
    AccuRevDepotManager getDepot(long depotId);
    AccuRevDepotManager createDepot(AccuRevProperties props);
    AccuRevDepotManager updateDepot(long depotId, AccuRevProperties props);
    void removeDepot(long depotId);
}