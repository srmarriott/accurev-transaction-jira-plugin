package issuetabpanels;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.core.util.StringUtils;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.util.JiraKeyUtils;
import com.github.srmarriott.jira.plugins.accurev.AccuRevTrans;
import com.github.srmarriott.jira.plugins.accurev.AccuRevVersion;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.AccuRevVersion.elementType;

/**
 * One item in the 'AccuRev Transactions' tab.
 */
public class AccuRevTransAction extends AbstractIssueAction {
	private static final Logger log = LoggerFactory.getLogger(AccuRevTransAction.class);
    private final AccuRevTrans transaction;
    private final long depotId;
    private final int realDepotId;
    private final String accuRevWebLink;
    protected final IssueTabPanelModuleDescriptor descriptor;
    protected MultipleAccuRevDepotManager multipleAccuRevDepotManager;

    public AccuRevTransAction(AccuRevTrans tmpTrans, MultipleAccuRevDepotManager multipleAccuRevDepotManager, IssueTabPanelModuleDescriptor descriptor, long depotId) {
        super(descriptor);
        this.multipleAccuRevDepotManager = multipleAccuRevDepotManager;
        this.descriptor = descriptor;
        /* SVN-93 */
        this.transaction = new AccuRevTrans(
                tmpTrans.getId(),
                tmpTrans.getType(),
                tmpTrans.getTime(),
                tmpTrans.getUser(),
                rewriteLogMessage(tmpTrans.getComment()),
                tmpTrans.getVersions());
        this.depotId = depotId;
        this.realDepotId = multipleAccuRevDepotManager.getDepot(depotId).getRealDepotId();
        this.accuRevWebLink = multipleAccuRevDepotManager.getDepot(depotId).getWebLink();
    }

    protected void populateVelocityParams(Map params) {
        params.put("stringUtils", new StringUtils());
        params.put("accurev", this);
    }
    
    public String getDepotDisplayName() {
        return multipleAccuRevDepotManager.getDepot(depotId).getDepotName();
    }

    public Date getTimePerformed() {
        if (transaction.getTime() == null) {
            throw new UnsupportedOperationException("no revision date for this log entry");
        }
        return transaction.getTime();
    }

    public long getRealDepotId() {
        return realDepotId;
    }
    
    public long getDepotId() {
        return depotId;
    }

    public String getUsername() {
        return transaction.getUser();
    }

    public AccuRevTrans getTransaction() {
        return transaction;
    }
    
    public String getTransLink(AccuRevTrans transaction) {
    	final String defaultHtml = "";
    	final String transViewFormat = accuRevWebLink + "/WebGui.jsp?tran_number=${transId}&depot=${depotId}&stream=${streamId}&view=trans_hist";
    	String href = transViewFormat;
    	
    	if (transaction.getStream().equals("") == false) {
    		href = StringUtils.replaceAll(href, "${streamId}", transaction.getStream());
    	} else {
    		return Long.toString(transaction.getId());
    	}
    	href = StringUtils.replaceAll(href, "${transId}", Long.toString(transaction.getId()));
    	href = StringUtils.replaceAll(href, "${depotId}", Long.toString(getRealDepotId()));
    	return "<a href=\"" + href + "\" target=\"_blank\">" + Long.toString(transaction.getId()) + "</a>";
    }
    
    public String getPathDiffLink(AccuRevTrans transaction, AccuRevVersion version) {
    	final String fileDiffFormat = accuRevWebLink + "/browse/${depotId}/${streamId}/${path}?eid=${eid}&v1=${v1}&v2=${v2}&view=diff";
    	String href = "";
    	if (version.getElemType() == elementType.TEXT || version.getElemType() == elementType.PTEXT) {
    		href = fileDiffFormat;
    	} else if (version.getElemType() == elementType.BINARY) {
    		return version.getPath(); // TODO
    	} else {
    		return version.getPath(); // TODO
    	}
    	
    	if (transaction.getStream().equals("") == false) {
    		href = StringUtils.replaceAll(href, "${streamId}", transaction.getStream());
    	} else {
    		return version.getPath();
    	}
    	
    	String virtualVer = version.getVirtualVer();
    	
    	String eid = version.getEid();
    	String virtualIdVer = virtualVer.split("/")[1];
        String streamId = virtualVer.split("/")[0];
        virtualVer = virtualVer.replace("/","%2F");
        String path = version.getPath();
        log.debug("path: " + path);
        path = path.replace("/./","");
        log.debug("path2: " + path);
        if (Integer.parseInt(virtualIdVer) > 1) {
        	String virtualVer1 = streamId + "%2F" + String.valueOf(Integer.parseInt(virtualIdVer) - 1);
        	href = StringUtils.replaceAll(href, "${v2}", virtualVer1);
        } else {
        	href = StringUtils.replaceAll(href, "${v2}", virtualVer);
        }
    	href = StringUtils.replaceAll(href, "${v1}", virtualVer);
    	href = StringUtils.replaceAll(href, "${eid}", eid);
    	href = StringUtils.replaceAll(href, "${path}", path);
    	href = StringUtils.replaceAll(href, "${depotId}", Long.toString(getRealDepotId()));
    	
    	return "<a href=\"" + href + "\" target=\"_blank\">" + version.getPath() + "</a>";
    }
    
    /**
     * Converts all lower case JIRA issue keys to upper case so that they can be
     * correctly rendered in the Velocity macro, makelinkedhtml.
     *
     * @param logMessageToBeRewritten
     * The SVN log message to be rewritten.
     * @return
     * The rewritten SVN log message.
     * @see
     * <a href="http://jira.atlassian.com/browse/SVN-93">SVN-93</a>
     */
    protected String rewriteLogMessage(final String commentToBeRewritten)
    {
        String comment = commentToBeRewritten;
        //final String commentUpperCase = StringUtils.upperCase(comment);
        final Set <String>issueKeys = new HashSet<String>(getIssueKeysFromComment(comment));

        for (String issueKey : issueKeys)
            comment = comment.replaceAll("(?ium)" + issueKey, issueKey);

        return comment;
    }

    List<String> getIssueKeysFromComment(String commentUpperCase)
    {
        return JiraKeyUtils.getIssueKeysFromString(commentUpperCase);
    }
}
