package com.github.srmarriott.jira.plugins.accurev.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;


public class DeleteAccuRevDepotAction extends AccuRevActionSupport {
	private final static Logger log = LoggerFactory.getLogger(DeleteAccuRevDepotAction.class);
	private long depotId;
	private AccuRevDepotManager accuRevManager;

	public DeleteAccuRevDepotAction(MultipleAccuRevDepotManager manager) {
		super(manager);
	}

	public String getDepotId() {
		return Long.toString(depotId);
	}

	public void setDepotId(String depotId) {
		this.depotId = Long.parseLong(depotId);
	}

	public String doDefault() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }
		accuRevManager = getMultipleAccuRevDepotManager().getDepot(depotId);
		getMultipleAccuRevDepotManager().removeDepot(depotId);
		//return getRedirect("ViewAccuRevDepots.jspa");
		return INPUT;
	}

	public void doValidation() {
		log.debug("DeleteAccuRevDepotAction: doValidation");
	}
	
	public String doExecute() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }
		getMultipleAccuRevDepotManager().removeDepot(depotId);
		return getRedirect("ViewAccuRevDepots.jspa");
	}

	public AccuRevDepotManager getAccuRevManager() {
		return accuRevManager;
	}
}
