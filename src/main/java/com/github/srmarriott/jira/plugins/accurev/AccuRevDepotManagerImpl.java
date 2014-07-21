package com.github.srmarriott.jira.plugins.accurev;

import com.atlassian.jira.util.JiraKeyUtils;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.util.TextUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.atlassian.core.exception.InfrastructureException;
import com.github.srmarriott.jira.plugins.accurev.AccuRevDepot;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

public class AccuRevDepotManagerImpl implements AccuRevDepotManager {
    private final static Logger log = LoggerFactory.getLogger(AccuRevDepotManagerImpl.class);
    private Map<Long, AccuRevTrans> transactionCache;
    private AccuRevDepot depot;
    private boolean active;
    private String inactiveMessage;
    private long id;
    private long latestTransaction; // The latest transaction from AccuRev when getTransactions was ran
    private long latestQueryTransaction; // The latest transaction hist went up to when getTransactions was ran
    private PropertySet properties;
    
    public AccuRevDepotManagerImpl(long id, PropertySet props) {
        this.id = id;
        this.properties = props;
        log.debug("Setting up new AccuRevDepotManagerImpl");
    	setup();
    }
    
    public void activate() {
        try {
            this.depot = new AccuRevDepot(
            		getServer(),
            		getPort(),
            		getDepotName(),
            		getUsername(),
            		AccuRevDepotManagerImpl.decryptPassword(getPassword()));
            if (depot.setDepotId() == false) {
            	throw new Exception("Error getting the depot name from AccuRev");
            }
            active = true;
        }
        catch (Exception e) {
            log.error("Connection to AccuRev depot " + getDepotName() + " failed: " + e, e);
            // We don't want to throw an exception here because then the system won't start if the server is down
            // or there is something wrong with the configuration.  We also still want this depot to show up
            // in our configuration so the user has a chance to fix the problem.
            active = false;
            inactiveMessage = e.getMessage();
        }
    }
    
    protected void setup() {
        // Now setup transaction indexing if they want it
            // Setup the log message cache
        int cacheSize = 10000;
        if (getTransactionCacheSize() > 0) {
            cacheSize = getTransactionCacheSize();
        }

        transactionCache = new LRUMap(cacheSize);
        activate();
    }
    
    public synchronized Collection<AccuRevTrans> getTransactions(long transaction) {
        final Collection<AccuRevTrans> transactions = new ArrayList();

        if (!isActive()) {
            return transactions;
        }

        latestTransaction = 1;
        try {
            latestTransaction = depot.getLatestTransactionId();
        }
        catch (AccuRevException e) {
            // connection was active, but apparently now it's not
            log.error("Non-zero return from AccuRev cmd", e);
            deactivate(e.getMessage());
            return transactions;
        }
        catch (IOException e) {
            log.error("IO error from cmd.", e);
            deactivate(e.getMessage());
            return transactions;
        }
        catch (InterruptedException e) {
            log.error("Interrupted error.", e);
            deactivate(e.getMessage());
            return transactions;
        }
        catch (SAXException e) {
            log.error("SAX error.", e);
            deactivate(e.getMessage());
            return transactions;
        }
        catch (ParserConfigurationException e) {
            log.error("Parser config error.", e);
            deactivate(e.getMessage());
            return transactions;
        }

        if (log.isDebugEnabled()) {
            log.debug("Latest revision in depot=" + getDepotName() + "  is : " + latestTransaction);
        }

        if (latestTransaction > 0 && latestTransaction < transaction) {
            if (log.isDebugEnabled()) {
                log.debug("Have all the transactions for depot=" + getDepotName() + " - doing nothing.");
            }
            return transactions;
        }
        
        long retrieveStart = transaction;
        if (retrieveStart <= 0) {
            retrieveStart = 1;
        }
        latestQueryTransaction = retrieveStart + MultipleAccuRevDepotManager.ACCUREV_MAX_TRANSACTION_UPDATE;
        if (latestQueryTransaction >= latestTransaction) {
        	latestQueryTransaction = latestTransaction;
        }

        if (log.isDebugEnabled()) {
            log.debug("Retrieving transactions to index (between " + retrieveStart + " and " + latestQueryTransaction + ") for depot=" + getDepotName());
        }

        try {
            Collection<AccuRevTrans> tmpTransactions = depot.histBuffered(retrieveStart, latestQueryTransaction);
            for (AccuRevTrans tmpTransaction : tmpTransactions) {
                if (log.isDebugEnabled()) {
                        log.debug("Retrieved #" + tmpTransaction.getId() + " : " + tmpTransaction.getComment());
                    }
                if (TextUtils.stringSet(tmpTransaction.getComment()) && JiraKeyUtils.isKeyInString(StringUtils.upperCase(tmpTransaction.getComment()))) {
                    transactions.add(tmpTransaction);
                }
            }
        }
        catch (Exception e) {
            log.error("Error retrieving transactions from the depot.", e);
            deactivate(e.getMessage());
        }
        if (log.isDebugEnabled()) {
            log.debug("Retrieved " + transactions.size() + " relevant transactions to index (between " + retrieveStart + " and " + latestQueryTransaction + ") from depot=" + getDepotName());
        }

        // temp log comment
        if (log.isDebugEnabled()) {
            log.debug("transactions size = " + transactions.size() + " for " + getDepotName());
        }
        return transactions;
    }
    
    public synchronized AccuRevTrans getTransaction(long transaction) {
        if (!isActive()) {
            throw new IllegalStateException("The connection to the repository is not active");
        }
        final AccuRevTrans[] transactions = new AccuRevTrans[]{(AccuRevTrans) transactionCache.get(new Long(transaction))};

        if (transactions[0] == null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("No cache - retrieving log message for transaction: " + transaction);
                }
                AccuRevTrans trans = depot.getTransaction(transaction);
                transactions[0] = trans;
                // TODO: port
                ensureCached(trans);
            } catch (Exception e) {
                log.error("Error retrieving logs: " + e, e);
                deactivate(e.getMessage());
                throw new InfrastructureException(e);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Found cached log message for revision: " + transaction);
        }
        return transactions[0];
    }

    public long getId() {
        return this.id;
    }
    
    public String getServer() {
        return properties.getString(MultipleAccuRevDepotManager.ACCUREV_SERVER);
    }

    public long getPort() {
        return properties.getLong(MultipleAccuRevDepotManager.ACCUREV_PORT);
    }
    
    public String getWebLink() {
        return properties.getString(MultipleAccuRevDepotManager.ACCUREV_WEBLINK);
    }
    
    public String getDepotName() {
        return properties.getString(MultipleAccuRevDepotManager.ACCUREV_DEPOT_NAME);
    }
    
    public String getUsername() {
        return properties.getString(MultipleAccuRevDepotManager.ACCUREV_USERNAME);
    }
    
    public String getPassword() {
        return properties.getString(MultipleAccuRevDepotManager.ACCUREV_PASSWORD);
    }
    
    public boolean isActive() {
        return active;
    }
    
    private void deactivate(String message) {
        if (depot != null) {
            depot = null;
        }
        active = false;
        inactiveMessage = message;
    }
    
    public static String decryptPassword(String encrypted) throws IOException {
        if (encrypted == null)
            return null;

        byte[] result = Base64.decodeBase64(encrypted.getBytes());

        return new String(result, 0, result.length);
    }
    
    public static String encryptPassword(String password) {
        if (password == null)
            return null;

        byte[] result = Base64.encodeBase64(password.getBytes());
        return new String(result, 0, result.length);
    }

    public String getInactiveMessage() {
        return inactiveMessage;
    }
    
    public int getTransactionCacheSize() {
        return properties.getInt(MultipleAccuRevDepotManager.ACCUREV_TRANSACTION_CACHE_SIZE_KEY);
    }
    
    public long getLatestTransactionFromLastCall() {
    	return latestTransaction;
    }
    
    public long getLatestQueryTransactionFromLastCall() {
    	return latestQueryTransaction;
    }
    
    private void ensureCached(AccuRevTrans transaction) {
        synchronized (transactionCache) {
            transactionCache.put(new Long(transaction.getId()), transaction);
        }
    }
    
    public long getLatestTransaction() {
    	long tmpLatestTransaction = 1;
        try {
            tmpLatestTransaction = depot.getLatestTransactionId();
            return tmpLatestTransaction;
        }
        catch (AccuRevException e) {
            // connection was active, but apparently now it's not
            log.error("Non-zero return from AccuRev cmd", e);
            deactivate(e.getMessage());
            return tmpLatestTransaction;
        }
        catch (IOException e) {
            log.error("IO error from cmd.", e);
            deactivate(e.getMessage());
            return tmpLatestTransaction;
        }
        catch (InterruptedException e) {
            log.error("Interrupted error.", e);
            deactivate(e.getMessage());
            return tmpLatestTransaction;
        }
        catch (SAXException e) {
            log.error("SAX error.", e);
            deactivate(e.getMessage());
            return tmpLatestTransaction;
        }
        catch (ParserConfigurationException e) {
            log.error("Parser config error.", e);
            deactivate(e.getMessage());
            return tmpLatestTransaction;
        }
    }
    
    public PropertySet getProperties() {
        return properties;
    }
    
    public synchronized void update(AccuRevProperties props) {
        deactivate(null);

        AccuRevProperties.Util.fillPropertySet(props, properties);

        setup();
    }
    
    public int getRealDepotId() {
    	return this.depot.getDepotId();
    }
}
