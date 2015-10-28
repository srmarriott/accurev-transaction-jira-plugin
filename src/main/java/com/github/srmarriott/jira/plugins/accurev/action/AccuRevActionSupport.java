package com.github.srmarriott.jira.plugins.accurev.action;

import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.velocity.htmlsafe.HtmlSafe;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the AccuRev plugins actions.
 */
public class AccuRevActionSupport extends JiraWebActionSupport {
	private final static Logger log = LoggerFactory.getLogger(AccuRevActionSupport.class);
	private MultipleAccuRevDepotManager multipleDepotManager;

	public AccuRevActionSupport(MultipleAccuRevDepotManager manager) {
		this.multipleDepotManager = manager;
	}

	protected MultipleAccuRevDepotManager getMultipleAccuRevDepotManager() {
		return multipleDepotManager;
	}

	public boolean hasPermissions() {
		return hasPermission(Permissions.ADMINISTER);
	}

	public String doDefault() {
		log.debug("Entering doDefault");
		if (!hasPermissions())
        {
			return PERMISSION_VIOLATION_RESULT;
		}

		return INPUT;
	}

    @HtmlSafe
    public String escapeJavaScript(String javascriptUnsafeString)
    {
        return StringEscapeUtils.escapeJavaScript(javascriptUnsafeString);
    }
}
