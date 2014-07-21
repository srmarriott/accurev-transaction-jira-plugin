package com.github.srmarriott.jira.plugins.accurev.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.AccuRevProperties;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;
import com.opensymphony.util.TextUtils;

public class AddAccuRevDepotAction extends AccuRevActionSupport implements AccuRevProperties {
	private final static Logger log = LoggerFactory.getLogger(AddAccuRevDepotAction.class);
	private String depotName;
	private String server;
	private long port;
	private String webLink;
	private String username;
	private String password;
	private Boolean transactionIndexing = Boolean.TRUE;
	private Integer transactionCacheSize = new Integer(10000);

	public AddAccuRevDepotAction(MultipleAccuRevDepotManager manager) {
		super(manager);
	}

	public void doValidation() {
		validateDepotParameters();
	}

	public String getDepotName() {
		return depotName;
	}
	
	public void setDepotName(String depotName) {
		this.depotName = depotName;
	}
	
	public String getServer() {
		return server;
	}
	
	public void setServer(String server) {
		this.server = server;
	}
	
	public long getPort() {
		return port;
	}
	
	public void setPort(long port) {
		this.port = port;
	}
	
	public String getWebLink() {
		return webLink;
	}
	
	public void setWebLink(String webLink) {
		this.webLink = webLink;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		if (TextUtils.stringSet(username)) {
			this.username = username;
		} else {
			this.username = null;
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (TextUtils.stringSet(password)) {
			this.password = password;
		} else {
			this.password = null;
		}
	}

	public Boolean getTransactionIndexing() {
		return transactionIndexing;
	}

	public void setTransactionIndexing(Boolean transactionIndexing) {
		this.transactionIndexing = transactionIndexing;
	}

	public Integer getTransactionCacheSize() {
		return transactionCacheSize;
	}

	public void setTransactionCacheSize(Integer transactionCacheSize) {
		this.transactionCacheSize = transactionCacheSize;
	}

	public String doExecute() throws Exception {
        if (!hasPermissions())
        {
            return PERMISSION_VIOLATION_RESULT;
        }
        log.debug("AddAccuRevDepotAction: doExecute");
		AccuRevDepotManager accuRevManager = getMultipleAccuRevDepotManager().createDepot(this);
		if (!accuRevManager.isActive()) {
			addErrorMessage(accuRevManager.getInactiveMessage());
			addErrorMessage(getText("admin.errors.occured.when.creating"));
			getMultipleAccuRevDepotManager().removeDepot(accuRevManager.getId());
			return ERROR;
		}

		return getRedirect("ViewAccuRevDepots.jspa");
	}

	public void validateDepotParameters() {
		if (!TextUtils.stringSet(getDepotName()))
			addError("depotName", getText("accurev.errors.you.must.specify.a.name.for.the.depot"));
		if (!TextUtils.stringSet(getServer()))
			addError("server", "You must specify a name for the server");
		if (!TextUtils.stringSet(getWebLink()))
			addError("webLink", "You must specify a web link.");
		if (!TextUtils.stringSet(getUsername()))
			addError("username", "You must specify a username.");
		if (!TextUtils.stringSet(getPassword()))
			addError("password", "You must specify a password.");
	}

}
