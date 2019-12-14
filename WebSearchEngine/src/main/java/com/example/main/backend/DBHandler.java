package com.example.main.backend;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import org.la4j.Vector;
import org.la4j.matrix.SparseMatrix;
import org.la4j.vector.DenseVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.example.main.backend.dao.DBResponseDocument;
import com.example.main.backend.pagerank.PageRank;
import com.example.main.backend.utils.Utils;

@Repository
public class DBHandler {

	@Autowired
	public DataSource dataSource;

	@Value("${bm25.k}")
	String bm25_k;

	@Value("${bm25.b}")
	String bm25_b;

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void updateScores() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL update_scores(?,?)");
		query.setFloat(1, Float.parseFloat(bm25_k));
		query.setFloat(2, Float.parseFloat(bm25_k));
		query.execute();
		con.close();
	}

	public SearchResultResponse searchConjunctiveQuery(String query, int a_k, SearchResultResponse a_response)
			throws SQLException {

		Connection con = getConnection();
		List<String> searchTerms = Utils.getTermsInQuotes(query);
		String[] searchTermsArr = Utils.getTermsInQuotes(query).toArray(new String[searchTerms.size()]);
		PreparedStatement sql = con.prepareStatement("SELECT * from get_docs_for_conjunctive_search(?)");
		sql.setArray(1, con.createArrayOf("text", searchTermsArr));
//		sql.setInt(2, a_k);
		sql.execute();
		ResultSet results = sql.getResultSet();
		return processSearchQueryResultSet(con, results, searchTerms, a_response); // closes connection too
	}

	public SearchResultResponse searchDisjunctiveQuery(String query, int a_k, SearchResultResponse a_response)
			throws SQLException {

		Connection con = getConnection();

		List<String> searchTerms = Utils.getTermsWithoutQuotes(query);
		String[] searchTermsArr = (String[]) searchTerms.toArray(new String[searchTerms.size()]);
		List<String> requiredTerms = Utils.getTermsInQuotes(query);
		String[] requiredTermsArr = (String[]) requiredTerms.toArray(new String[requiredTerms.size()]);

		PreparedStatement sql = con.prepareStatement("SELECT * from get_docs_for_disjunctive_search(?,?)");
		sql.setArray(1, con.createArrayOf("text", searchTermsArr));
		sql.setArray(2, con.createArrayOf("text", requiredTermsArr));
//		sql.setInt(3, a_k); apply  Limit it in Java now.
		sql.execute();
		ResultSet results = sql.getResultSet();
		searchTerms.addAll(requiredTerms); // combine all terms
		return processSearchQueryResultSet(con, results, searchTerms, a_response); // closes connection too
	}

	private SearchResultResponse processSearchQueryResultSet(Connection con, ResultSet results,
			List<String> searchTerms, SearchResultResponse a_response) throws SQLException {

		HashMap<String, DBResponseDocument> resDocs = new HashMap<String, DBResponseDocument>();

		while (results.next()) {
			String url = results.getString(2);
			String term = results.getString(3);
			float score_tfidf = results.getFloat(4);
			float score_okapi = results.getFloat(5);
			if (!resDocs.containsKey(url))
				resDocs.put(url, new DBResponseDocument(url));
			DBResponseDocument doc = resDocs.get(url);
			doc.add_term(term, score_tfidf, score_okapi);
		}
		results.close();
		con.close();

		DBResponseDocument queryDoc = new DBResponseDocument(null);

		for (String t : searchTerms)
			queryDoc.add_term(t, 1, 1);

		ArrayList<DBResponseDocument> sortedSet = new ArrayList<DBResponseDocument>();
		for (String key : resDocs.keySet()) {
			DBResponseDocument doc = resDocs.get(key);
			double similarityScore = queryDoc.getCosineSimilarity_tfIdf(doc);
			doc.cosSim = similarityScore;
			sortedSet.add(doc);
		}

		Collections.sort(sortedSet, (d1, d2) -> Double.compare(d1.cosSim, d2.cosSim));

		int rank = 1;
		if (a_response == null)
			a_response = new SearchResultResponse();
		for (DBResponseDocument resDoc : sortedSet)
			a_response.addSearchResultItem(rank++, resDoc.url, (float) resDoc.cosSim);
		return a_response;
	}

	public SearchResultResponse getStats(String query, SearchResultResponse a_response) throws SQLException {

		List<String> terms = Utils.getTermsWithoutQuotes(query);
		terms.addAll(Utils.getTermsInQuotes(query));
		String[] termsArr = (String[]) terms.toArray(new String[terms.size()]);

		Connection con = getConnection();
		PreparedStatement sql = con.prepareStatement("SELECT * from get_term_frequencies(?)");
		sql.setArray(1, con.createArrayOf("text", termsArr));
		sql.execute();
		ResultSet results = sql.getResultSet();

		if (a_response == null)
			a_response = new SearchResultResponse();

		while (results.next()) {
			String term = results.getString(1);
			int df = results.getInt(2);
			a_response.addStat(df, term);
		}

		results.close();
		con.close();
		return a_response;
	}

	public int getCollectionSize() throws SQLException {
		Connection con = getConnection();
		PreparedStatement sql = con.prepareStatement("SELECT COUNT(docid) from documents");
		sql.execute();
		ResultSet results = sql.getResultSet();
		results.next();
		int retVal = results.getInt(1);
		results.close();
		con.close();
		return retVal;
	}

	/**
	 * Save current state of the crawler into the database
	 */
	public void cancel(int maximumDepth, int maximumNumberOfDocs, int crawledDocuments, boolean leaveDomain,
			int parallelism) {

		Connection con = null;
		try {

			con = getConnection();
			PreparedStatement clearCrawlerState = con.prepareStatement("TRUNCATE TABLE crawlerState RESTART IDENTITY");
			clearCrawlerState.execute();
			clearCrawlerState.close();

			PreparedStatement ps = con.prepareStatement(
					"INSERT INTO crawlerState (maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism) VALUES (?,?,?,?,?)");
			ps.setInt(1, maximumDepth);
			ps.setInt(2, maximumNumberOfDocs);
			ps.setInt(3, crawledDocuments);
			ps.setBoolean(4, leaveDomain);
			ps.setInt(5, parallelism);

			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * Gets the next URL from the database
	 * 
	 * @return null if there are no more URLs in the database, otherwise the URL and
	 *         the depth of that URL in the hierarchy
	 * @throws SQLException
	 * @throws URISyntaxException
	 */
	public Object[] getNextURL() throws SQLException, URISyntaxException {
		Connection con = null;
		try {
			con = getConnection();
			PreparedStatement stmtNextURL = con
					.prepareStatement("SELECT * FROM crawlerQueue ORDER BY id FETCH FIRST ROW ONLY");
			stmtNextURL.execute();
			ResultSet res = stmtNextURL.getResultSet();
			if (res.next()) {
				PreparedStatement stmnt = con.prepareStatement("DELETE FROM crawlerQueue WHERE id = ?");
				stmnt.setInt(1, res.getInt(1));
				stmnt.execute();
				stmnt.close();
				try {
					return new Object[] { new URL(res.getString(2)), res.getInt(3) };
				} catch (MalformedURLException e) {
					return null;
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		}
		return null;

	}

	/**
	 * Inserts the provided URLs into the database if they are not already listed
	 * 
	 * @param urls URLs to insert
	 * @param con  Connection to the database
	 * @throws SQLException
	 * @throws MalformedURLException
	 */
	public void queueURLs(Set<URL> urls) throws SQLException, MalformedURLException {

		Connection con = null;
		try {
			con = getConnection();
			PreparedStatement stmtCheckIfExists = con
					.prepareStatement("SELECT count(url) FROM documents WHERE url LIKE ? GROUP BY url" + "	UNION "
							+ "SELECT count(url) FROM crawlerQueue WHERE url LIKE ? GROUP BY url");

			// Check if URL is already crawled. If so --> don't process it further
			// Reduces the number of accesses to a single domain

			Set<URL> urlsAlreadyCrawled = new HashSet<URL>();
			for (URL url : urls) {
				stmtCheckIfExists.setString(1, url.toString());
				stmtCheckIfExists.setString(2, url.toString());

				stmtCheckIfExists.execute();
				ResultSet s = stmtCheckIfExists.getResultSet();
				while (s.next()) {
					if (s.getInt(1) > 0) {
						urlsAlreadyCrawled.add(url);
					}
				}
			}

			for (URL url : urlsAlreadyCrawled) {
				urls.remove(url);
			}

			PreparedStatement stmtQueueURLs = con
					.prepareStatement("INSERT INTO crawlerQueue(id, url, current_depth) VALUES (DEFAULT, ?, ?)");

			for (URL url : urls) {
				stmtQueueURLs.setString(1, url.toString());
				stmtQueueURLs.setInt(2, 0);
				stmtQueueURLs.addBatch();
			}

			stmtQueueURLs.executeBatch();

			// If conflict on unique constraint url occurs --> ignore conflict and do
			// nothing
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO documents (docid, url,crawled_on_date, language, page_rank) VALUES (DEFAULT,?,NULL,NULL,NULL) ON CONFLICT DO NOTHING");

			for (URL url : urls) {
				stmt.setString(1, url.toString());
				stmt.addBatch();
			}

			stmt.executeBatch();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public void insertDocDataToDatabase(HTMLDocument doc, Connection con) throws SQLException, MalformedURLException {

		PreparedStatement stmtUpdateDoc = con
				.prepareStatement("UPDATE documents SET crawled_on_date = CURRENT_DATE, language = ? WHERE url LIKE ?");
		stmtUpdateDoc.setString(1, doc.getLanguage());
		stmtUpdateDoc.setString(2, doc.getUrl().toString());
		stmtUpdateDoc.executeUpdate();

		PreparedStatement stmtgetDocId = con.prepareStatement("SELECT docid FROM documents WHERE url LIKE ?");
		stmtgetDocId.setString(1, doc.getUrl().toString());
		stmtgetDocId.execute();
		ResultSet key = stmtgetDocId.getResultSet();
		if (key.next()) {
			int docId = key.getInt(1);

			// Insert features of document
			PreparedStatement stmtInsertFeature = con
					.prepareStatement("INSERT INTO features (docid, term, term_frequency) VALUES (?,?,?)");
			for (Entry<String, Integer> e : doc.getTermFrequencies().entrySet()) {
				stmtInsertFeature.setInt(1, docId);
				stmtInsertFeature.setString(2, e.getKey());
				stmtInsertFeature.setInt(3, e.getValue());
				stmtInsertFeature.addBatch();
			}
			stmtInsertFeature.executeBatch();
			stmtInsertFeature.close();

			PreparedStatement setNumberOfTerms = con
					.prepareStatement("UPDATE documents SET num_of_terms = ? WHERE docid = ?");
			setNumberOfTerms.setInt(1, doc.getTermFrequencies().entrySet().size());
			setNumberOfTerms.setInt(2, docId);
			setNumberOfTerms.executeUpdate();

			// Insert blank documents
			PreparedStatement stmtInsertBlankDocument = con.prepareStatement(
					"INSERT INTO documents (docid, url, crawled_on_date, language, page_rank) VALUES (DEFAULT, ?, NULL, NULL, NULL) ON CONFLICT DO NOTHING");
			for (URL url : doc.getLinks()) {
				stmtInsertBlankDocument.setString(1, url.toString());
				stmtInsertBlankDocument.addBatch();
			}
			stmtInsertBlankDocument.executeBatch();

			List<Integer> docKeys = new ArrayList<>();
			PreparedStatement stmtDocId = con.prepareStatement("SELECT docid FROM documents WHERE url LIKE ?");

			for (URL url : doc.getLinks()) {
				stmtDocId.setString(1, url.toString());
				stmtDocId.execute();
				ResultSet set = stmtDocId.getResultSet();
				if (set.next()) {
					docKeys.add(set.getInt(1));
				}
			}

			// Insert outgoing links
			PreparedStatement stmtInsertLinks = con
					.prepareStatement("INSERT INTO links(from_docid, to_docid) VALUES (?,?)");
			for (int toDoc : docKeys) {
				stmtInsertLinks.setInt(1, docId);
				stmtInsertLinks.setInt(2, toDoc);
				stmtInsertLinks.addBatch();
			}
			stmtInsertLinks.executeBatch();
			stmtInsertBlankDocument.close();
			stmtInsertLinks.close();
		}

		stmtUpdateDoc.close();
	}

	public void insertURLToVisited(URL url, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmt = con
				.prepareStatement("INSERT INTO crawlerVisitedPages (url, last_visited) VALUES (?, CURRENT_DATE)");
		stmt.setString(1, url.toString());
		stmt.execute();
		stmt.close();
	}

	public void insertURLSToQueue(Set<URL> urls, int currentDepth, Connection con) throws SQLException {

		PreparedStatement stmtgetDocId = con
				.prepareStatement("SELECT count(docid) FROM documents WHERE url LIKE ? AND crawled_on_date = NULL");

		PreparedStatement stmt = con.prepareStatement("INSERT INTO crawlerQueue (url, current_depth) VALUES (?,?)");
		for (URL url : urls) {
			stmtgetDocId.setString(1, url.toString());
			stmtgetDocId.execute();
			ResultSet s = stmtgetDocId.getResultSet();
			s.next();
			if (s.getInt(1) == 0) {
				stmt.setString(1, url.toString());
				stmt.setInt(2, currentDepth);
				stmt.addBatch();
			}

		}
		stmt.executeBatch();
		stmt.close();
	}

	public boolean getCrawlerFlag() throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmtCrawlerFlag = con.prepareStatement("SELECT run FROM crawlerState");
		stmtCrawlerFlag.execute();
		ResultSet s = stmtCrawlerFlag.getResultSet();
		boolean run = false;
		if (s.next()) {
			run = s.getBoolean(1);
		}
		s.close();
		stmtCrawlerFlag.close();
		con.close();
		return run;
	}

	public void setCrawlerFlag(boolean run) throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmt = con.prepareStatement("UPDATE crawlerState SET run = ?");
		stmt.setBoolean(1, run);
		stmt.execute();
		stmt.close();
		con.close();
	}

	public void insertCrawlerStateIfNotExists(int maximumDepth, int maximumDocs, int crawledDocs, boolean leaveDomain,
			int parallelism, boolean run, String[] domains) throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmtExists = con.prepareStatement("SELECT count(*) FROM crawlerState");
		stmtExists.execute();
		ResultSet s = stmtExists.getResultSet();
		if (s.next()) {
			int exists = s.getInt(1);
			if (exists == 0) {
				PreparedStatement insert = con.prepareStatement(
						"INSERT INTO crawlerState (maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism, run, domains) VALUES (?,?,?,?,?,?,?)");
				insert.setInt(1, maximumDepth);
				insert.setInt(2, maximumDocs);
				insert.setInt(3, crawledDocs);
				insert.setBoolean(4, leaveDomain);
				insert.setInt(5, parallelism);
				insert.setBoolean(6, run);
				Array domainArray = con.createArrayOf("TEXT", domains);
				insert.setArray(7, domainArray);
				insert.execute();
				insert.close();
			}
		}
		s.close();
		stmtExists.close();
		con.close();
	}

	public void saveCrawlerState(int maximumDepth, int maximumDocs, int crawledDocs, boolean leaveDomain,
			int parallelism, boolean run, String[] domains) throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmtTrun = con.prepareStatement("TRUNCATE TABLE crawlerState");
		stmtTrun.execute();
		stmtTrun.close();
		con.close();
		insertCrawlerStateIfNotExists(maximumDepth, maximumDocs, crawledDocs, leaveDomain, parallelism, run, domains);
	}

	public Object[] loadCrawlerState() throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmt = con.prepareStatement(
				"SELECT maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism, run, domains FROM crawlerState");
		stmt.execute();
		ResultSet r = stmt.getResultSet();
		if (r.next()) {
			return new Object[] { r.getInt(1), r.getInt(2), r.getInt(3), r.getBoolean(4), r.getInt(5), r.getBoolean(6),
					(String[]) r.getArray(7).getArray() };
		}
		return null;
	}

	public boolean firstStartupCrawler() throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmtExists = con.prepareStatement("SELECT count(*) FROM crawlerState");
		stmtExists.execute();
		ResultSet s = stmtExists.getResultSet();
		s.next();
		boolean firstStartup = (0 == s.getInt(1));
		s.close();
		stmtExists.close();
		con.close();
		return firstStartup;
	}

	public void computePageRank(double randomJumpProbability, double terminationCriteria) throws SQLException {
		// TODO: Extend database schema
		Connection con = this.getConnection();
		con.setAutoCommit(false);
		// PreparedStatement outgoingEdges = con.prepareStatement("SELECT from_docid,
		// to_docid FROM links");
		PreparedStatement edges = con.prepareStatement(
				"SELECT d1.docid, d2.docid, (SELECT EXISTS (SELECT 1 FROM links WHERE from_docid = d1.docid AND to_docid = d2.docid)), (SELECT count(to_docid) FROM links WHERE from_docid = d1.docid) FROM documents d1, documents d2 ORDER BY d1.docid, d2.docid");
		PreparedStatement docCount = con.prepareStatement("SELECT count(docid) FROM documents");

		// Build matrix
		docCount.execute();
		ResultSet countResult = docCount.getResultSet();
		countResult.next();
		int vertices = countResult.getInt(1);
		countResult.close();
		docCount.close();

		// Matrix initialized with zeros
		SparseMatrix tm = SparseMatrix.zero(vertices, vertices);
		edges.execute();
		ResultSet r = edges.getResultSet();
		int row = -1;
		int column = -1;
		int lastDocRow = -1;
		int lastDocColumn = -1;

		ArrayList<Integer> docIds = new ArrayList<Integer>();

		while (r.next()) {
			int docRow = r.getInt(1);
			int docColumn = r.getInt(2);
			boolean edgeExists = r.getBoolean(3);
			int outDegree = r.getInt(4);

			if (docRow > lastDocRow) {
				// Ensures that every docId is only inserted once
				// No need to sort them later, because the query sorts them
				docIds.add(docRow);
				lastDocRow = docRow;
				row++;
			}
			if (docColumn < lastDocColumn) {
				lastDocColumn = -1;
				column = 0;
			}else if (docColumn > lastDocColumn) {
				lastDocColumn = docColumn;
				column++;
			}

			if (edgeExists) {
				tm.set(row, column, ((double) 1) / outDegree);
			} else if (outDegree == 0) {
				tm.set(row, column, ((double) 1) / vertices);
			}
		}

		r.close();
		edges.close();
		con.commit();
		con.close();

		// Transition matrix is ready --> now compute
		PageRank pr = new PageRank.Builder().withRandomJumpProability(0.1).withTerminationCriteria(0.001)
				.withTransitionMatrix(tm).build();

		Vector pageRanks = pr.getStationaryDistribution();
		for (int x = 0; x < pageRanks.length(); x++) {
			this.setPageRank(docIds.get(x), pageRanks.get(x));
		}

		System.out.println("PageRank computed");
	}

	public void setPageRank(int docId, double pagerank) throws SQLException {
		Connection con = this.getConnection();
		PreparedStatement stmt = con.prepareStatement("UPDATE documents SET page_rank = ? WHERE docid = ?");
		stmt.setDouble(1, pagerank);
		stmt.setInt(2, docId);
		stmt.execute();
		stmt.close();
		con.close();
	}
}