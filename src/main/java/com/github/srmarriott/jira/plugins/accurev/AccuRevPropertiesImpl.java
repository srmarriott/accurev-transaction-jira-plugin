package com.github.srmarriott.jira.plugins.accurev;

public class AccuRevPropertiesImpl implements AccuRevProperties {
    private String depotName;
    private String server;
    private long port;
    private String webLink;
    private String username;
    private String password;
    private Boolean transactionIndexing;
    private Integer transactionCacheSize;
    
    public AccuRevPropertiesImpl(
    		String depotName,
    		String server,
    		Long port,
    		String webLink,
    		String username,
    		String password,
    		Boolean transactionIndexing,
    		Integer transactionCacheSize) {
    	this.depotName = depotName;
    	this.server = server;
    	this.port = port;
    	this.username = username;
    	this.password = password;
    	this.webLink = webLink;
    	this.transactionIndexing = transactionIndexing;
    	this.transactionCacheSize = transactionCacheSize;
    }
    
    public void fillPropertiesFromOther(AccuRevProperties other) {
	    if (this.getUsername() == null) {
	        this.username = other.getUsername();
	    }
	    
	    if (this.getPassword() == null) {
	        this.password = other.getPassword();
	    }
	    
	    if (this.getTransactionIndexing() == null) {
	        this.transactionIndexing = other.getTransactionIndexing();
	    }
	    
	    if (this.getTransactionCacheSize() == null) {
	        this.transactionCacheSize = other.getTransactionCacheSize();
	    }
    }
    
    public String toString() {
        return "username: " + getUsername() + " password: " + getPassword() + " transactionIndex: " + getTransactionIndexing() + " transactionCacheSize: " + getTransactionCacheSize();
    }
    
    public String getDepotName() {
        return depotName;
    }
    
    public String getServer() {
        return server;
    }

    public long getPort() {
        return port;
    }
    
    public String getWebLink() {
        return webLink;
    }
    
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getTransactionIndexing() {
        return transactionIndexing;
    }

    public Integer getTransactionCacheSize() {
        return transactionCacheSize;
    }

    public AccuRevProperties setDepotName(String depot) {
        this.depotName = depot;
        return this;
    }
    
    public AccuRevProperties setServer(String server) {
        this.server = server;
        return this;
    }
    
    public AccuRevProperties setPort(long port) {
        this.port = port;
        return this;
    }
    
    public AccuRevProperties setWebLink(String webLink) {
        this.webLink = webLink;
        return this;
    }

    public AccuRevProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public AccuRevProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public AccuRevProperties setRevisionIndexing(Boolean revisionIndexing) {
        this.transactionIndexing = revisionIndexing;
        return this;
    }

    public AccuRevProperties setRevisioningCacheSize(Integer revisioningCacheSize) {
        this.transactionCacheSize = revisioningCacheSize;
        return this;
    }
}