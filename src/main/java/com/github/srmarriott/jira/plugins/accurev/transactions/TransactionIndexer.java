package com.github.srmarriott.jira.plugins.accurev.transactions;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.core.exception.InfrastructureException;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.JiraKeyUtils;
import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.AccuRevException;
import com.github.srmarriott.jira.plugins.accurev.AccuRevTrans;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.PermittedIssuesRevisionFilter;
import com.github.srmarriott.jira.plugins.accurev.ProjectTransactionFilter;
import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionIndexer {
    // VARIABLES
    private final static Logger log = LoggerFactory.getLogger(TransactionIndexer.class);
    private static final Long NOT_INDEXED = -1L;
//   	<interface>com.atlassian.sal.api.lifecycle.LifecycleAware</interface> this goes to atlassian-plugin.xml
    static final String TRANSACTIONS_INDEX_DIRECTORY = "accurev-transactions";

    // These are names of the fields in the Lucene documents that contain transaction info.
    public static final String FIELD_ID = "id";
    public static final Term START_TRANSACTION = new Term(FIELD_ID, "");
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_USER = "user";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_ISSUEKEY = "issuekey";
    public static final String FIELD_PROJECTKEY = "project";
    public static final String FIELD_DEPOT = "depot";
    public static final String FIELD_STREAM = "stream";

    public static final StandardAnalyzer ANALYZER = new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_30);

    public static final int MAX_REVISIONS = 100;

    private final MultipleAccuRevDepotManager multipleAccuRevDepotManager;
    private final VersionManager versionManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final ChangeHistoryManager changeHistoryManager;
    private final IndexPathManager indexPathManager;
    private Hashtable<Long, Long> latestIndexedTransactionTbl;
    private LuceneIndexAccessor indexAccessor;
    
    public TransactionIndexer(
    		MultipleAccuRevDepotManager multipleAccuRevDepotManager,
    		VersionManager versionManager,
    		IssueManager issueManager,
    		PermissionManager permissionManager,
    		ChangeHistoryManager changeHistoryManager,
    		IndexPathManager indexPathManager) {
        this.multipleAccuRevDepotManager = multipleAccuRevDepotManager;
        this.versionManager = versionManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.changeHistoryManager = changeHistoryManager;
        this.indexAccessor = new DefaultLuceneIndexAccessor();
        this.indexPathManager = indexPathManager;
        initializeLatestIndexedTransactionCache();
    }
    
    public void start() {
        try {
            createIndexIfNeeded();
        }
        catch (Exception e) {
            log.error("Error installing the revision index service.", e);
            throw new InfrastructureException("Error installing the revision index service.", e);
        }
    }
    
    /**
     * Looks for the revision index directory and creates it if it does not already exist.
     *
     * @return Return <tt>true</tt> if the index directory is usable or created; <tt>false</tt> otherwise.
     */
    private boolean createIndexIfNeeded()
    {
        if (log.isDebugEnabled())
            log.debug("TransactionIndexer.createIndexIfNeeded()");

        boolean indexExists = indexDirectoryExists();
        log.debug("indexExists: " + indexExists);
        if (getIndexPath() != null && !indexExists)
        {
            try
            {
                indexAccessor.getIndexWriter(getIndexPath(), true, ANALYZER).close();
                initializeLatestIndexedTransactionCache();
                return true;
            }
            catch (IOException ioe)
            {
                log.error("There's a performing IO on the index.", ioe);
                return false;
            }
        }
        else
        {
            return indexExists;
        }
    }
    
    private void initializeLatestIndexedTransactionCache()
    {
        Collection<AccuRevDepotManager> depots = multipleAccuRevDepotManager.getDepotList();

        latestIndexedTransactionTbl = new Hashtable<Long, Long>();

        for (AccuRevDepotManager currentDepot : depots)
            initializeLatestIndexedTransactionCache(currentDepot);

        if (log.isDebugEnabled())
            log.debug("Number of depots: " + depots.size());
    }

    private void initializeLatestIndexedTransactionCache(AccuRevDepotManager accuRevDepotManager)
    {
        latestIndexedTransactionTbl.put(accuRevDepotManager.getId(), NOT_INDEXED);
    }

    private boolean indexDirectoryExists()
    {
        try
        {
            // check if the directory exists
            File file = new File(getIndexPath());

            return file.exists();
        }
        catch (Exception e)
        {
            return false;
        }
    }
    
    public String getIndexPath()
    {
        String indexPath = null;
        String rootIndexPath = indexPathManager.getPluginIndexRootPath();
        if (rootIndexPath != null)
        {
            indexPath = rootIndexPath + System.getProperty("file.separator") + TRANSACTIONS_INDEX_DIRECTORY;
        }
        else
        {
            log.warn("At the moment the root index path of jira is not set, so we can not form an index path for the subversion plugin.");
        }

        return indexPath;
    }
    
    /**
     * This method updates the index, creating it if it does not already exist.
     * TODO: this monster really needs to be broken down - weed out the loop control
     *
     * @throws IndexException if there is some problem in the indexing subsystem meaning indexes cannot be updated.
     */
    public void updateIndex() throws IndexException, IOException
    {
        if (createIndexIfNeeded())
        {
            Collection<AccuRevDepotManager> depots = multipleAccuRevDepotManager.getDepotList();

            // temp log comment
            if (log.isDebugEnabled())
                log.debug("Number of depots: " + depots.size());

            for (AccuRevDepotManager accuRevDepotManager : depots)
            {
                try
                {
                    // if the depot isn't active, try activating it. if it still not accessible, skip it
                    if (!accuRevDepotManager.isActive())
                    {
                        accuRevDepotManager.activate();

                        if (!accuRevDepotManager.isActive())
                        {
                            continue;
                        }
                    }

                    long depotId = accuRevDepotManager.getId();
                    long latestIndexedTransaction = -1;

                    if (getLatestIndexedTransaction(depotId) != null) {
                        latestIndexedTransaction = getLatestIndexedTransaction(depotId);
                    }
                    else {
                        // no latestIndexedTransaction, no need to update? This probably means
                        // that the repository have been removed from the file system
                        log.warn("Did not update index because null value in hash table for " + depotId);
                        continue;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("Updating revision index for depot=" + depotId);
                    }

                    if (latestIndexedTransaction < 0)
                    {
                        latestIndexedTransaction = updateLastTransactionIndexed(depotId);
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("Latest indexed revision for depot=" + depotId + " is : " + latestIndexedTransaction);
                    }

                    @SuppressWarnings("unchecked")
                    final Collection<AccuRevTrans> transactions = accuRevDepotManager.getTransactions(latestIndexedTransaction + 1);
                    log.debug("Collection size: " + transactions.size());
                    
                    long latestQueryTransaction = accuRevDepotManager.getLatestQueryTransactionFromLastCall();
                    if (!accuRevDepotManager.isActive()) {
                    	continue;
                    }
                    IndexWriter writer = indexAccessor.getIndexWriter(getIndexPath(), false, ANALYZER);
                    
                    if (latestIndexedTransaction <= 0) {
                    	latestIndexedTransaction = 1;
                    } else {
                    	latestIndexedTransaction += 1;
                    }
                    if (transactions.isEmpty() == true) {
                    	latestIndexedTransaction = latestQueryTransaction;
                		latestIndexedTransactionTbl.put(depotId, latestIndexedTransaction);
                		log.debug("latestIndexedTransaction: " + latestIndexedTransaction);
                    }

                    try {
                        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
                        try {
                            for (AccuRevTrans transaction : transactions) {
                                if (TextUtils.stringSet(transaction.getComment()) && isKeyInString(transaction)) {
                                    if (!hasDocument(depotId, transaction.getId(), reader)) {
                                        Document doc = getDocument(depotId, transaction);
                                        if (log.isDebugEnabled()) {
                                            log.debug("Indexing depot=" + depotId + ", transaction: " + transaction.getId());
                                        }
                                        writer.addDocument(doc);
                                        log.debug("transaction.getId(): " + transaction.getId() + ", latest: " + latestIndexedTransaction);
                                    }
                                }
                                if (transaction.getId() > latestIndexedTransaction) {
                                	latestIndexedTransaction = latestQueryTransaction;
                            		latestIndexedTransactionTbl.put(depotId, latestIndexedTransaction);
                            		log.debug("latestIndexedTransaction: " + latestIndexedTransaction);
                                }
                            }
                        } finally {
                            reader.close();
                        }
                    } finally {
                        writer.close();
                    }
                }
                catch (IOException e) {
                    log.warn("Unable to index depot '" + accuRevDepotManager.getDepotName() + "'", e);
                }
                catch (RuntimeException e) {
                    log.warn("Unable to index depot '" + accuRevDepotManager.getDepotName() + "'", e);
                }
            }  // while
        }
    }
    
    protected boolean isKeyInString(AccuRevTrans transaction)
    {
        final String logMessageUpperCase = StringUtils.upperCase(transaction.getComment());
        return JiraKeyUtils.isKeyInString(logMessageUpperCase);
    }
    
    protected Long getLatestIndexedTransaction(long depotId)
    {
    	log.debug("latestIndexedTransactionTbl: " + latestIndexedTransactionTbl.size());
        return latestIndexedTransactionTbl.get(depotId);
    }
    
    /**
     * Work out whether a given change, for the specified repository, is already in the index or not.
     */
    private boolean hasDocument(long depotId, long transaction, IndexReader reader) throws IOException
    {
        IndexSearcher searcher = new IndexSearcher(reader);
        try
        {
            TermQuery depotQuery = new TermQuery(new Term(FIELD_DEPOT, Long.toString(depotId)));
            TermQuery transQuery = new TermQuery(new Term(FIELD_ID, Long.toString(transaction)));
            BooleanQuery depotAndTransQuery = new BooleanQuery();

            depotAndTransQuery.add(depotQuery, BooleanClause.Occur.MUST);
            depotAndTransQuery.add(transQuery, BooleanClause.Occur.MUST);

            TopDocs hits = searcher.search(depotAndTransQuery, MAX_REVISIONS);

            if (hits.totalHits == 1)
            {
                return true;
            }
            else if (hits.totalHits == 0)
            {
                return false;
            }
            else
            {
                log.error("Found MORE than one document for transaction: " + transaction + ", depot=" + depotId);
                return true;
            }
        }
        finally
        {
            searcher.close();
        }
    }
    
    private long updateLastTransactionIndexed(Long depotId) throws IndexException, IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Updating last transaction indexed.");
        }

        // find all log entries that have already been indexed for the specified depot
        // (i.e. all logs that have been associated with issues in JIRA)
        long latestIndexedTransaction = latestIndexedTransactionTbl.get(depotId);

        String indexPath = getIndexPath();
        final IndexReader reader;
        try
        {
            reader = IndexReader.open(FSDirectory.open(new File(indexPath), new SimpleFSLockFactory()));
        }
        catch (IOException e)
        {
            log.error("Problem with path " + indexPath + ": " + e.getMessage(), e);
            throw new IndexException("Problem with path " + indexPath + ": " + e.getMessage(), e);
        }
        IndexSearcher searcher = new IndexSearcher(reader);

        try
        {
            TopDocs hits = searcher.search(new TermQuery(new Term(FIELD_DEPOT, Long.toString(depotId))), MAX_REVISIONS);

            for (int i = 0; i < Math.min(hits.totalHits, MAX_REVISIONS); ++i)
            {
                Document doc = searcher.doc(hits.scoreDocs[i].doc);
                final long transaction = Long.parseLong(doc.get(FIELD_ID));
                log.debug("searcher: " + transaction);
                if (transaction > latestIndexedTransaction)
                {
                    latestIndexedTransaction = transaction;
                }

            }
            log.debug("latestIndRev for " + depotId + " = " + latestIndexedTransaction);
            latestIndexedTransactionTbl.put(depotId, latestIndexedTransaction);
        }
        finally
        {
            reader.close();
        }
        searcher.close();
        return latestIndexedTransaction;
    }

    /**
     * Creates a new Lucene document for the supplied log entry. This method is used when indexing
     * revisions, not during retrieval.
     *
     * @param repoId   ID of the repository that contains the revision
     * @param logEntry The subversion log entry that is about to be indexed
     * @return A Lucene document object that is ready to be added to an index
     */
    protected Document getDocument(long depotId, AccuRevTrans transaction)
    {
        Document doc = new Document();

        // revision information
        doc.add(new Field(FIELD_COMMENT, transaction.getComment(), Field.Store.YES, Field.Index.NOT_ANALYZED));

        if (transaction.getUser() != null)
        {
            doc.add(new Field(FIELD_USER, transaction.getUser(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        doc.add(new Field(FIELD_DEPOT, Long.toString(depotId), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_ID, Long.toString(transaction.getId()), Field.Store.YES, Field.Index.NOT_ANALYZED));

        if (transaction.getTime() != null)
        {
            doc.add(new Field(FIELD_TIME, DateField.dateToString(transaction.getTime()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        // relevant issue keys
        List<String> keys = getIssueKeysFromString(transaction);

        // Relevant project keys. Used to avoid adding duplicate projects.
        Map<String, String> projects = new HashMap<String, String>();

        for (String issueKey : keys)
        {
            doc.add(new Field(FIELD_ISSUEKEY, issueKey, Field.Store.YES, Field.Index.NOT_ANALYZED));
            String projectKey = getProjectKeyFromIssueKey(issueKey);
            if (!projects.containsKey(projectKey))
            {
                projects.put(projectKey, projectKey);
                doc.add(new Field(FIELD_PROJECTKEY, projectKey, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
        }

        return doc;
    }
    
    protected String getProjectKeyFromIssueKey(String issueKey)
    {
        final String issueKeyUpperCase = StringUtils.upperCase(issueKey);
        return JiraKeyUtils.getFastProjectKeyFromIssueKey(issueKeyUpperCase);
    }
    
    protected List<String> getIssueKeysFromString(AccuRevTrans transaction)
    {
        final String logMessageUpperCase = StringUtils.upperCase(transaction.getComment());
        return JiraKeyUtils.getIssueKeysFromString(logMessageUpperCase);
    }

    public Map<Long, List<AccuRevTrans>> getTransactionsByDepot(Issue issue) throws IndexException, IOException
    {
        return getTransactionsByDepot(issue, 0, MAX_REVISIONS, true);
    }

    /**
     * Gets the commits relevant to the specified issue from all the configured depots.
     *
     * @param issue      The issue to get entries for.
     * @param startIndex For paging &mdash; The index of the entry that is the first result in the page desired.
     * @param pageSize   For paging &mdash; The size of the page.
     * @return A {@link java.util.Map} of {@com.atlassian.jira.plugin.ext.accurev.AccuRevDepotManager} IDs to the commits in them
     *         that relate to the issue.
     * @throws IndexException Thrown if there's a getting a reader to the index.
     * @throws IOException    Thrown if there's a problem reading the index.
     */
    public Map<Long, List<AccuRevTrans>> getTransactionsByDepot(Issue issue, int startIndex, int pageSize, boolean ascending) throws IndexException, IOException
    {
        if (log.isDebugEnabled())
            log.debug("Retrieving transactions for : " + issue.getKey());


        if (!indexDirectoryExists())
        {
            log.warn("The indexes for the accurev plugin have not yet been created.");
            return null;
        }
        else
        {
            final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
            IndexSearcher searcher = new IndexSearcher(reader);

            try
            {
                TopDocs hits = searcher.search(createQueryByIssueKey(issue),  MAX_REVISIONS, new Sort(new SortField(FIELD_TIME, SortField.STRING, !ascending)));
                Map<Long, List<AccuRevTrans>> transactions = new LinkedHashMap<Long, List<AccuRevTrans>>(hits.totalHits);

                int endIndex = startIndex + pageSize;

                // SVN-370 - Prevent ArrayIndexOutOfBoundsException when more than 100 commits (which is MAX_REVISIONS) are to be shown
                for (int i = 0; i < Math.min(hits.totalHits, MAX_REVISIONS); i++)
                {
                    if (i < startIndex || i >= endIndex)
                        continue;

                    Document doc = searcher.doc(hits.scoreDocs[i].doc);
                    long depotId = Long.parseLong(doc.get(FIELD_DEPOT));//repositoryId is UUID + location
                    log.debug("depotId: " + depotId);
                    AccuRevDepotManager manager = multipleAccuRevDepotManager.getDepot(depotId);
                    if (manager == null) {
                    	log.debug("manager is null!");
                    }
                    long transaction = Long.parseLong(doc.get(FIELD_ID));
                    AccuRevTrans logEntry = manager.getTransaction(transaction);
                    if (logEntry == null) {
                        log.error("Could not find log message for transaction: " + Long.parseLong(doc.get(FIELD_ID)));
                    }
                    else {
                        // Look for list of map entries for repository
                        List<AccuRevTrans> entries = transactions.get(depotId);
                        if (entries == null) {
                            entries = new ArrayList<AccuRevTrans>();
                            transactions.put(depotId, entries);
                        }
                        entries.add(logEntry);
                    }
                }

                return transactions;
            }
            finally {
                searcher.close();
                reader.close();
            }
        }
    }
    
    public Map<Long, List<AccuRevTrans>> getTransactionsByDepot(String projectKey, ApplicationUser user, int startIndex, int pageSize) throws IndexException, IOException
    {
        if (!indexDirectoryExists())
        {
            log.warn("getLogEntriesByProject() The indexes for the subversion plugin have not yet been created.");
            return null;
        }
        else
        {

            // Set up and perform a search for all documents having the supplied projectKey,
            // sorted in descending date order
            TermQuery query = new TermQuery(new Term(FIELD_PROJECTKEY, projectKey));

            Map<Long, List<AccuRevTrans>> transactions;
            final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
            IndexSearcher searcher = new IndexSearcher(reader);

            try
            {
                TopDocs hits = searcher.search(query, new ProjectTransactionFilter(issueManager, permissionManager, user, projectKey),  MAX_REVISIONS, new Sort(new SortField(FIELD_TIME, SortField.LONG, true)));

                if (hits == null)
                {
                    log.info("getLogEntriesByProject() No matches -- returning null.");
                    return null;
                }
                // Build the result map
                transactions = new LinkedHashMap<Long, List<AccuRevTrans>>();
                int endIndex = startIndex + pageSize;

                for (int i = 0, j = Math.min(hits.totalHits, MAX_REVISIONS); i < j; ++i)
                {
                    if (i < startIndex || i >= endIndex)
                        continue;

                    Document doc = searcher.doc(hits.scoreDocs[i].doc);

                    long depotId = Long.parseLong(doc.get(FIELD_DEPOT)); //depotId is UUID + location
                    AccuRevDepotManager manager = multipleAccuRevDepotManager.getDepot(depotId);
                    long transactionId = Long.parseLong(doc.get(FIELD_ID));
                    AccuRevTrans transaction = manager.getTransaction(transactionId);
                    if (transaction == null)
                    {
                        log.error("getLogEntriesByProject() Could not find log message for transaction: " + transactionId);
                        continue;
                    }
                    // Look up the list of map entries for this repository. Create one if needed
                    List<AccuRevTrans> entries = transactions.get(depotId);
                    if (entries == null)
                    {
                        entries = new ArrayList<AccuRevTrans>();
                        transactions.put(depotId, entries);
                    }

                    // Add this entry
                    entries.add(transaction);
                }
            }
            finally
            {
                searcher.close();
                reader.close();
            }

            return transactions;
        }
    }

    public Map<Long, List<AccuRevTrans>> getTransactionsByVersion(Version version, ApplicationUser user, int startIndex, int pageSize) throws IndexException, IOException
    {
        if (!indexDirectoryExists())
        {
            log.warn("getLogEntriesByVersion() The indexes for the subversion plugin have not yet been created.");
            return null;
        }

        // Find all isuses affected by and fixed by any of the versions:
        Collection<Issue> issues = new HashSet<Issue>();

        issues.addAll(versionManager.getIssuesWithFixVersion(version));
        issues.addAll(versionManager.getIssuesWithAffectsVersion(version));

        // Construct a query with all the issue keys. Make sure to increase the maximum number of clauses if needed.
        int maxClauses = BooleanQuery.getMaxClauseCount();
        if (issues.size() > maxClauses)
            BooleanQuery.setMaxClauseCount(issues.size());

        BooleanQuery query = new BooleanQuery();
        Set<String> permittedIssueKeys = new HashSet<String>();

        for (Issue issue : issues)
        {
            String key = issue.getString(FIELD_ISSUEKEY);
            Issue theIssue = issueManager.getIssueObject(key);

            if (permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, theIssue, user))
            {
                TermQuery termQuery = new TermQuery(new Term(FIELD_ISSUEKEY, key));
                query.add(termQuery, BooleanClause.Occur.SHOULD);
                permittedIssueKeys.add(key);
            }
        }

        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
        IndexSearcher searcher = new IndexSearcher(reader);
        Map<Long, List<AccuRevTrans>> logEntries;

        try
        {
            // Run the query and sort by date in descending order
            TopDocs hits = searcher.search(query, new PermittedIssuesRevisionFilter(issueManager, permissionManager, user, permittedIssueKeys), MAX_REVISIONS, new Sort(new SortField(FIELD_TIME, SortField.LONG, true)));

            if (hits == null)
            {
                log.info("getLogEntriesByVersion() No matches -- returning null.");
                return null;
            }

            logEntries = new LinkedHashMap<Long, List<AccuRevTrans>>();
            int endDocIndex = startIndex + pageSize;

            for (int i = 0, j = Math.min(hits.totalHits, MAX_REVISIONS); i < j; ++i)
            {
                if (i < startIndex || i >= endDocIndex)
                    continue;

                Document doc = searcher.doc(hits.scoreDocs[i].doc);
                long depotId = Long.parseLong(doc.get(FIELD_DEPOT));//repositoryId is UUID + location
                AccuRevDepotManager manager = multipleAccuRevDepotManager.getDepot(depotId);
                long transaction = Long.parseLong(doc.get(FIELD_ID));

                AccuRevTrans logEntry = manager.getTransaction(transaction);
                if (logEntry == null)
                {
                    log.error("getLogEntriesByVersion() Could not find log message for revision: " + Long.parseLong(doc.get(FIELD_ID)));
                }
                // Add the entry to the list of map entries for the repository. Create a new list if needed
                List<AccuRevTrans> entries = logEntries.get(depotId);
                if (entries == null)
                {
                    entries = new ArrayList<AccuRevTrans>();
                    logEntries.put(depotId, entries);
                }
                entries.add(logEntry);
            }
        }
        finally
        {
            searcher.close();
            reader.close();
            BooleanQuery.setMaxClauseCount(maxClauses);
        }

        return logEntries;
    }
    
    public void addDepot(AccuRevDepotManager accuRevInstance)
    {
        initializeLatestIndexedTransactionCache(accuRevInstance);
        try
        {
            updateIndex();
        }
        catch (Exception e)
        {
            throw new InfrastructureException("Could not index repository", e);
        }
    }
    
    public void removeEntries(AccuRevDepotManager accuRevInstance) throws IOException, IndexException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Deleting transactions for : " + accuRevInstance.getDepotName() + ", " + accuRevInstance.getId());
        }

        if (!indexDirectoryExists())
        {
            log.warn("The indexes for the AccuRev plugin have not yet been created.");
        }
        else
        {
            long depotId = accuRevInstance.getId();

            IndexWriter writer = null;

            try
            {
                writer = indexAccessor.getIndexWriter(getIndexPath(), false, ANALYZER);

                writer.deleteDocuments(new Term(FIELD_DEPOT, Long.toString(depotId)));
                initializeLatestIndexedTransactionCache(accuRevInstance);
            }
            catch (IOException ie)
            {
                if (log.isErrorEnabled())
                    log.error("Unable to open index. " +
                            "Perhaps the index is corrupted. It might be possible to fix the problem " +
                            "by removing the index directory (" + getIndexPath() + ")", ie);

                throw ie; /* Rethrow for normal error handling? SVN-200 */
            }
            finally
            {
                if (null != writer)
                {
                    try
                    {
                        writer.close();
                    }
                    catch (IOException ioe)
                    {
                        if (log.isWarnEnabled())
                            log.warn("Unable to close index.", ioe);
                    }
                }

            }
        }
    }
   
    protected Query createQueryByIssueKey(Issue issue)
    {
        BooleanQuery query = new BooleanQuery();

        // add current key
        query.add(new TermQuery(new Term(FIELD_ISSUEKEY, issue.getKey())), BooleanClause.Occur.SHOULD);

        // add all previous keys
        Collection<String> previousIssueKeys = changeHistoryManager.getPreviousIssueKeys(issue.getId());
        for (String previousIssueKey : previousIssueKeys)
        {
            TermQuery termQuery = new TermQuery(new Term(FIELD_ISSUEKEY, previousIssueKey));
            query.add(termQuery, BooleanClause.Occur.SHOULD);
        }

        return query;
    }
}
