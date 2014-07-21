package com.github.srmarriott.jira.plugins.accurev;
import com.opensymphony.module.propertyset.PropertySet;

public interface AccuRevProperties {
    String getUsername();
    String getServer();
    long getPort();
    String getWebLink();
    String getPassword();
    Boolean getTransactionIndexing();
    Integer getTransactionCacheSize();
    String getDepotName();

    static class Util {
        public static PropertySet fillPropertySet(AccuRevProperties properties, PropertySet propertySet) {
            propertySet.setString(MultipleAccuRevDepotManager.ACCUREV_DEPOT_NAME, properties.getDepotName());
            propertySet.setString(MultipleAccuRevDepotManager.ACCUREV_SERVER, properties.getServer());
            propertySet.setLong(MultipleAccuRevDepotManager.ACCUREV_PORT, properties.getPort());
            propertySet.setString(MultipleAccuRevDepotManager.ACCUREV_WEBLINK, properties.getWebLink());
            propertySet.setString(MultipleAccuRevDepotManager.ACCUREV_USERNAME, properties.getUsername());
            propertySet.setString(MultipleAccuRevDepotManager.ACCUREV_PASSWORD, AccuRevDepotManagerImpl.encryptPassword(properties.getPassword()));
            propertySet.setBoolean(MultipleAccuRevDepotManager.ACCUREV_TRANSACTION_INDEXING_KEY, properties.getTransactionIndexing().booleanValue());
            propertySet.setInt(MultipleAccuRevDepotManager.ACCUREV_TRANSACTION_CACHE_SIZE_KEY, properties.getTransactionCacheSize().intValue());
            return propertySet;
        }
    }
}