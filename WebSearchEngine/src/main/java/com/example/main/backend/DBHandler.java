package com.example.main.backend;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.la4j.matrix.SparseMatrix;
import org.la4j.vector.DenseVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.example.main.backend.pagerank.PageRank;

@Repository
public class DBHandler {

	@Autowired
	public DataSource dataSource;

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void updateScores() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL update_scores(?,?)");
		query.setFloat(1, (float) 1.2);
		query.setFloat(2, (float) 0.75);
		query.execute();
		con.close();
	}

	public SearchResultResponse searchConjunctiveQuery(String query, int a_k, SearchResultResponse a_response)
			throws SQLException {

		Connection con = getConnection();
		List<String> searchTerms = getTermsInQuotes(query);
		String[] searchTermsArr = getTermsInQuotes(query).toArray(new String[searchTerms.size()]);
		PreparedStatement sql = con.prepareStatement("SELECT * from conjunctive_search(?, ?)");
		sql.setArray(1, con.createArrayOf("text", searchTermsArr));
		sql.setInt(2, a_k);
		sql.execute();
		ResultSet results = sql.getResultSet();
		if (a_response == null)
			a_response = new SearchResultResponse();
		int rank = 1;
		while (results.next()) {
			String url = results.getString(1);
			float score = results.getFloat(2);
			a_response.addSearchResultItem(rank++, url, score);
		}
		results.close();
		con.close();
		return a_response;
	}

	public SearchResultResponse searchDisjunctiveQuery(String query, int a_k, SearchResultResponse a_response)
			throws SQLException {

		Connection con = getConnection();

		List<String> searchTerms = new ArrayList<String>();
		for (String subQuery : getTermsInQuotes("\"" + query + "\"")) {
			for (String term : subQuery.split(" ")) {
				term = term.trim();
				if (term.length() > 0) {
					searchTerms.add(term);
					System.out.println(term);
				}
			}
		}

		String[] searchTermsArr = (String[]) searchTerms.toArray(new String[searchTerms.size()]);
		List<String> requiredTerms = getTermsInQuotes(query);
		String[] requiredTermsArr = (String[]) requiredTerms.toArray(new String[requiredTerms.size()]);

		PreparedStatement sql = con.prepareStatement("SELECT * from disjunctive_search(?,?,?)");
		sql.setArray(1, con.createArrayOf("text", searchTermsArr));
		sql.setArray(2, con.createArrayOf("text", requiredTermsArr));
		sql.setInt(3, a_k);
		sql.execute();
		ResultSet results = sql.getResultSet();
		if (a_response == null)
			a_response = new SearchResultResponse();
		int rank = 1;
		while (results.next()) {
			String url = results.getString(1);
			float score = results.getFloat(2);
			a_response.addSearchResultItem(rank++, url, score);
		}
		results.close();
		con.close();
		return a_response;
	}

	public SearchResultResponse getStats(String query, SearchResultResponse a_response) throws SQLException {

		Connection con = getConnection();

		List<String> terms = new ArrayList<String>();
		for (String subQuery : getTermsInQuotes("\"" + query + "\"")) {
			for (String term : subQuery.split(" ")) {
				term = term.trim();
				if (term.length() > 0) {
					terms.add(term);
					System.out.println(term);
				}
			}
		}

		terms.addAll(getTermsInQuotes(query));

		String[] termsArr = (String[]) terms.toArray(new String[terms.size()]);

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

	public List<String> getTermsInQuotes(String query) {

		List<String> terms = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\"([^\"]*)\"");
		Matcher matcher = pattern.matcher(query);
		while (matcher.find()) {
			String term = matcher.group(1);
			terms.add(term);
			System.out.println(term);
		}

		return terms;
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
		System.out.println(vertices);
		SparseMatrix tm = SparseMatrix.zero(vertices, vertices);

		edges.execute();
		System.out.println("EXECUTED THAT BIG THING");
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
				//Ensures that every docId is only inserted once
				//No need to sort them later, because the query sorts them
				docIds.add(docRow);
				row++;
			}
			if (docColumn > lastDocColumn) {
				column++;
			}

			if (edgeExists) {
				tm.set(row, column, ((double) 1) / outDegree);
			} else if (outDegree == 0) {
				tm.set(row, column, ((double) 1) / vertices);
			}
			System.out.println("NEXT " + row + " " + column);
		}
		
		r.close();
		edges.close();
		con.commit();
		con.close();
		
		// Transition matrix is ready --> now compute
		PageRank pr = new PageRank.Builder().withRandomJumpProability(0.1).withTerminationCriteria(0.001)
				.withTransitionMatrix(tm).build();

		DenseVector pageRanks = pr.getStationaryDistribution();
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