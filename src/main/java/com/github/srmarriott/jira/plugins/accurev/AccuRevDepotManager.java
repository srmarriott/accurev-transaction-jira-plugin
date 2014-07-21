package com.github.srmarriott.jira.plugins.accurev;

import java.util.Collection;
import com.opensymphony.module.propertyset.PropertySet;

public interface AccuRevDepotManager {
    Collection<AccuRevTrans> getTransactions(long trans);
    AccuRevTrans getTransaction(long trans);
    String getDepotName();
    String getWebLink();
    String getServer();
    long getPort();
    long getId();
    String getUsername();
    String getPassword();
    boolean isActive();
    String getInactiveMessage();
    void activate();
    int getTransactionCacheSize();
    long getLatestTransaction();
	PropertySet getProperties();
    void update(AccuRevProperties properties);
    int getRealDepotId();
    long getLatestTransactionFromLastCall();
    long getLatestQueryTransactionFromLastCall();
}