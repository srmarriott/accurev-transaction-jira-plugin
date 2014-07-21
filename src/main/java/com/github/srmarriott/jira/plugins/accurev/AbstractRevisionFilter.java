package com.github.srmarriott.jira.plugins.accurev;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import com.github.srmarriott.jira.plugins.accurev.transactions.TransactionIndexer;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.search.Filter;

public abstract class AbstractRevisionFilter extends Filter
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -4608022736312175670L;

	final IssueManager issueManager;

    final PermissionManager permissionManager;

    final User user;

    final FieldSelector issueKeysFieldSelector;

    public AbstractRevisionFilter(IssueManager issueManager, PermissionManager permissionManager, User user)
    {
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.user = user;
        issueKeysFieldSelector = new FieldSelector()
        {
            /**
			 * 
			 */
			private static final long serialVersionUID = 995674386489294458L;

			public FieldSelectorResult accept(String s)
            {
                return StringUtils.equals(s, TransactionIndexer.FIELD_ISSUEKEY)
                        ? FieldSelectorResult.LOAD
                        : FieldSelectorResult.NO_LOAD;
            }
        };
    }
}
