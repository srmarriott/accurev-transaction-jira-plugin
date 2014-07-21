package com.github.srmarriott.jira.plugins.accurev.transactions;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

public interface LuceneIndexAccessor {
    /**
     * Gets a Lucene {@link org.apache.lucene.index.IndexReader} at the given path.
     *
     * @param path the path.
     * @return the IndexReader.
     * @throws IOException if there's some problem getting the reader.
     */
    IndexReader getIndexReader(String path) throws IOException;

    /**
     * Gets a Lucene {@link org.apache.lucene.index.IndexWriter} at the given path.
     *
     * @param path the path.
     * @param create if true, then create if absent.
     * @param analyzer the {@link org.apache.lucene.analysis.Analyzer} to use.
     * @throws IOException if there's some problem getting the writer.
     * @return the IndexWriter.
     */
    IndexWriter getIndexWriter(String path, boolean create, Analyzer analyzer)  throws IOException;
}

