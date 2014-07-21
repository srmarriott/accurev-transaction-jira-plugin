package com.github.srmarriott.jira.plugins.accurev.action;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;

public class ActivateAccuRevDepotAction extends AccuRevActionSupport {
	private long depotId;
	private AccuRevDepotManager accuRevDepotManager;

	public ActivateAccuRevDepotAction(MultipleAccuRevDepotManager manager) {
		super(manager);
	}

	public String getRepoId() {
		return Long.toString(depotId);
	}

	public void setRepoId(String depotId) {
		this.depotId = Long.parseLong(depotId);
	}

	public String doExecute() {
        if (!hasPermissions())
        {
            return PERMISSION_VIOLATION_RESULT;
        }

		accuRevDepotManager = getMultipleAccuRevDepotManager().getDepot(depotId);
		accuRevDepotManager.activate();
		if (!accuRevDepotManager.isActive()) {
			addErrorMessage(getText("accurev.depot.activation.failed", accuRevDepotManager.getInactiveMessage()));
		}
		return SUCCESS;
	}

	public AccuRevDepotManager getAccuRevDepotManager() {
		return accuRevDepotManager;
	}

}
