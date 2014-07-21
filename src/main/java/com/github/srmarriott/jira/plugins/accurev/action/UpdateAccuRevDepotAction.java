package com.github.srmarriott.jira.plugins.accurev.action;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;


public class UpdateAccuRevDepotAction extends AddAccuRevDepotAction {
	private long depotId = -1;

	public UpdateAccuRevDepotAction(MultipleAccuRevDepotManager multipleAccuRevDepotManager) {
		super(multipleAccuRevDepotManager);
	}

	public String doDefault() {
		if (ERROR.equals(super.doDefault()))
			return ERROR;

        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }


        if (depotId == -1) {
			addErrorMessage(getText("accurev.depot.id.missing"));
			return ERROR;
		}

		// Retrieve the accurev depot
		final AccuRevDepotManager depot = getMultipleAccuRevDepotManager().getDepot(depotId);
		if (depot == null) {
			addErrorMessage(getText("accurev.depot.does.not.exist", Long.toString(depotId)));
			return ERROR;
		}

		this.setDepotName(depot.getDepotName());
		this.setServer(depot.getServer());
		this.setPort(depot.getPort());
		this.setWebLink(depot.getWebLink());
		this.setUsername(depot.getUsername());
		this.setPassword(depot.getPassword());
		this.setTransactionCacheSize(new Integer(depot.getTransactionCacheSize()));
		this.setTransactionIndexing(true);

		return INPUT;
	}

	public String doExecute() {
		if (!hasPermissions()) {
			addErrorMessage(getText("accurev.admin.privilege.required"));
			return ERROR;
		}

		if (depotId == -1) {
			return getRedirect("ViewAccuRevDepots.jspa");
		}

		AccuRevDepotManager accuRevDepotManager = getMultipleAccuRevDepotManager().updateDepot(depotId, this);
		if (!accuRevDepotManager.isActive()) {
			depotId = accuRevDepotManager.getId();
			addErrorMessage(accuRevDepotManager.getInactiveMessage());
			addErrorMessage(getText("admin.errors.occured.when.updating"));
			return ERROR;
		}
		return getRedirect("ViewAccuRevDepots.jspa");
	}

	public long getdepotId() {
		return depotId;
	}

	public void setdepotId(long depotId) {
		this.depotId = depotId;
	}

}