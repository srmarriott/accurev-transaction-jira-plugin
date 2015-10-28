package com.github.srmarriott.jira.plugins.accurev;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import com.github.srmarriott.jira.plugins.accurev.transactions.TransactionIndexer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.DocIdBitSet;

import java.io.IOException;
import java.util.BitSet;
import java.util.Set;

public class PermittedIssuesRevisionFilter extends AbstractRevisionFilter
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Set<String> permittedIssueKeys;

    public PermittedIssuesRevisionFilter(IssueManager issueManager, PermissionManager permissionManager, ApplicationUser user, Set<String> permittedIssueKeys)
    {
        super(issueManager, permissionManager, user);
        this.permittedIssueKeys = permittedIssueKeys;
    }

    @Override
    public DocIdSet getDocIdSet(IndexReader indexReader) throws IOException
    {
        BitSet bitSet = new BitSet(indexReader.maxDoc());

        for (String issueKey : permittedIssueKeys)
        {
            TermDocs termDocs = indexReader.termDocs(new Term(TransactionIndexer.FIELD_ISSUEKEY, issueKey));

            while (termDocs.next())
                bitSet.set(termDocs.doc(), true);
        }

        return new DocIdBitSet(bitSet);
    }
}
