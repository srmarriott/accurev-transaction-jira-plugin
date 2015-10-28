package issuetabpanels;


import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.EasyList;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.github.srmarriott.jira.plugins.accurev.AccuRevCli;
import com.github.srmarriott.jira.plugins.accurev.AccuRevDepot;
import com.github.srmarriott.jira.plugins.accurev.AccuRevTrans;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.ActionContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AccuRevTransactionsTabPanel extends AbstractIssueTabPanel implements IssueTabPanel
{
    private static final Logger log = LoggerFactory.getLogger(AccuRevTransactionsTabPanel.class);
    private final MultipleAccuRevDepotManager multipleAccuRevDepotManager;
    private final PermissionManager permissionManager;
    private final VelocityRequestContextFactory requestContextFactory;
    
    /**
     * The number of commits to show in the tab initially. 100 should be good enough for most issues.
     */
    public static final int NUMBER_OF_REVISIONS = 100;

    public AccuRevTransactionsTabPanel(MultipleAccuRevDepotManager multipleAccuRevDepotManager, PermissionManager permissionManager, VelocityRequestContextFactory requestContextFactory) {
        this.multipleAccuRevDepotManager = multipleAccuRevDepotManager;
        this.permissionManager = permissionManager;
        this.requestContextFactory = requestContextFactory;
    }

    @Override
    public boolean showPanel(Issue issue, ApplicationUser remoteUser)
    {
        return true;
    }
    
    public List getActions(Issue issue, ApplicationUser remoteUser)
    {

        try
        {
            // SVN-392 - Temporary setting to descending by default until JRA-30220 is fixed
            final boolean sortAscending = false;

            int pageSize = getPageSizeRequestParameter();
            //multipleAccuRevDepotManager.getTransactionIndexer().updateIndex();
            Map<Long, List<AccuRevTrans>> transactions = multipleAccuRevDepotManager.getTransactionIndexer().getTransactionsByDepot(
                    issue,
                    getPageRequestParameter() * pageSize,
                    pageSize + 1,
                    sortAscending
            );

            if (transactions == null)
            {
                GenericMessageAction action = new GenericMessageAction(getText("no.index.error.message"));
                return EasyList.build(action);
            }
            else if (transactions.isEmpty())
            {
                GenericMessageAction action = new GenericMessageAction(getText("no.log.entries.message"));
                return EasyList.build(action);
            }
            else
            {
                List<AccuRevTransAction> actions = new ArrayList<AccuRevTransAction>();

                for (Map.Entry<Long, List<AccuRevTrans>> entry : transactions.entrySet())
                	for (AccuRevTrans transaction : entry.getValue())
                        actions.add(createAccuRevTransAction(entry.getKey(), transaction));

                if (!sortAscending)
                    Collections.reverse(actions);

                /*
                 * Hack! If we have more than a page of actions, that means we should show the 'More' button.
                 */
                if (!actions.isEmpty() && actions.size() > pageSize)
                {
                    /**
                     * ViewIssue will reverse the list of actions if the action sort order is descending, so we
                     * need to sublist based on the order.
                     */

                    actions = sortAscending ? actions.subList(0, pageSize) : actions.subList(1, actions.size());

                    int lastActionIndex = sortAscending ? actions.size() - 1 : 0;
                    AccuRevTransAction lastAction = actions.get(lastActionIndex);

                    /**
                     * The last action should have specialized class name so that we can use it to tell us when
                     * to render the more button.
                     */
                    actions.set(
                            lastActionIndex,
                            createLastAccuRevTransactionActionInPage(
                                    lastAction.getDepotId(),
                                    lastAction.getTransaction()
                            )
                    );
                }
                return actions;
            }
        }
        catch (IndexException ie)
        {
            log.error("There's a problem with the Subversion index.", ie);
        }
        catch (IOException ioe)
        {
            log.error("Unable to read Subversion index.", ioe);
        }

        return Collections.emptyList();
    }
        
    private int getPageRequestParameter()
    {
        HttpServletRequest req = ActionContext.getRequest();

        if (null != req)
        {
            String pageIndexString = req.getParameter("pageIndex");
            return StringUtils.isBlank(pageIndexString) ? 0 : Integer.parseInt(pageIndexString);
        }

        return 0;
    }
    
    private int getPageSizeRequestParameter()
    {
        HttpServletRequest req = ActionContext.getRequest();

        if (null != req)
        {
            String pageIndexString = req.getParameter("pageSize");
            return StringUtils.isBlank(pageIndexString) ? NUMBER_OF_REVISIONS : Integer.parseInt(pageIndexString);
        }
        return NUMBER_OF_REVISIONS;
    }
    
    private String getText(String key)
    {
        return descriptor.getI18nBean().getText(key);
    }
    
    public AccuRevTransAction createAccuRevTransAction(long depotId, AccuRevTrans transaction)
    {
        return new AccuRevTransAction(transaction, multipleAccuRevDepotManager, descriptor, depotId);
    }

    public AccuRevTransAction createLastAccuRevTransactionActionInPage(long depotId, AccuRevTrans transaction)
    {
        return new LastAccuRevTransActionInPage(transaction, multipleAccuRevDepotManager, descriptor, depotId);
    }
    
    /**
     * A class specifically created for its unique name so that the action view VMs know that
     * the action it is processing is the last one and render a 'More' button.
     */
    private class LastAccuRevTransActionInPage extends AccuRevTransAction
    {
        public LastAccuRevTransActionInPage(AccuRevTrans transaction, MultipleAccuRevDepotManager multipleAccuRevDepotManager, IssueTabPanelModuleDescriptor descriptor, long depotId)
        {
            super(transaction, multipleAccuRevDepotManager, descriptor, depotId);
        }
    }
}

// TODO: debug code
//List<GenericMessageAction> actions = new ArrayList<GenericMessageAction>();
//
//for (Map.Entry<Long, List<AccuRevTrans>> entry : transactions.entrySet())
//    for (AccuRevTrans transaction : entry.getValue())
//        actions.add(new GenericMessageAction(Long.toString(transaction.getId()) + ", comment=" + transaction.getComment()));
//
//if (!sortAscending)
//    Collections.reverse(actions);
//
//return actions;