package com.example.main.backend;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.la4j.Vector;
import org.la4j.matrix.SparseMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.main.backend.api.SearchAPI;
import com.example.main.backend.api.responseObjects.Ad;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.example.main.backend.dao.DBResponseDocument;
import com.example.main.backend.dao.HTMLDocument;
import com.example.main.backend.dao.Snippet;
import com.example.main.backend.pagerank.PageRank;
import com.example.main.backend.utils.QueryExpansion;
import com.example.main.backend.utils.QueryParser;
import com.example.main.backend.utils.SnippetGenerator;
import com.example.main.backend.utils.Stemmer;
import com.example.main.backend.utils.Utils;

import net.sf.extjwnl.JWNLException;

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

	public void updateIdfScores() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL update_idf_scores_function()");
		query.execute();
		con.close();
	}

	public void updateStats() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL update_doc_stats_table()");
		query.execute();
		con.close();
	}

	public void updateDocFrequencies() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL update_df_table()");
		query.execute();
		con.close();
	}

	public void updateScores() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL update_scores(?,?)");
		query.setFloat(1, Float.parseFloat(bm25_k));
		query.setFloat(2, Float.parseFloat(bm25_b));
		query.execute();
		con.close();
	}

	public SearchResultResponse searchConjunctiveQuery(String query, int k_limit, String[] languages,
			SearchResultResponse a_response, int searchMode) throws SQLException {

		Connection con = getConnection();
		List<String> searchTerms = new ArrayList<String>();
		if (languages.length == 1 && languages[0].equalsIgnoreCase(HTMLDocument.Language.ENGLISH)) {
			for (String t : QueryParser.getTermsInQuotes(query)) {
				searchTerms.add(Utils.toStemmed(t));
			}
		} else {
			searchTerms = QueryParser.getTermsInQuotes(query);
		}
		String[] searchTermsArr = QueryParser.getTermsInQuotes(query).toArray(new String[searchTerms.size()]);
		PreparedStatement sql = con.prepareStatement("SELECT * from get_docs_for_conjunctive_search(?,?)");
		sql.setArray(1, con.createArrayOf("text", searchTermsArr));
		sql.setArray(2, con.createArrayOf("text", languages));
		sql.execute();
		ResultSet results = sql.getResultSet();
		return processSearchQueryResultSet(con, results, searchTerms, searchTerms, k_limit, a_response, 3, searchMode); // closes
		// connection
		// too
	}

	public SearchResultResponse searchDisjunctiveQuery(String query, int k_limit, String[] languages,
			SearchResultResponse a_response, int scoringMethod, int searchMode) throws SQLException, JWNLException {

		Connection con = getConnection();
		query = query.toLowerCase();

		List<List<String>> searchTerms = QueryParser.getTermsWithoutQuotes(query);
		List<String> expandedQueryTerms = QueryExpansion.expandQuery(searchTerms.get(QueryParser.TILDA_TERMS),
				searchTerms.get(QueryParser.NON_TILDA_TERMS));
		// if(language == "German")
		// expandedQueryTerms =
		// QueryExpansion.expandGermanQuery(searchTerms.get(QueryParser.TILDA_TERMS),
		// searchTerms.get(QueryParser.NON_TILDA_TERMS), con);

		String[] expandedQueryTermsArr = (String[]) expandedQueryTerms.toArray(new String[expandedQueryTerms.size()]);

		List<String> requiredTerms = QueryParser.getTermsInQuotes(query);
		String[] requiredTermsArr = new String[requiredTerms.size()];
		for (int i = 0; i < requiredTerms.size(); i++)
			requiredTermsArr[i] = Utils.stemTerm(requiredTerms.get(i));

		PreparedStatement sql = null;
		if (searchMode == SearchAPI.DOCUMENT_MODE) {
			sql = con.prepareStatement("SELECT * from get_docs_for_disjunctive_search(?,?,?)");
		} else if (searchMode == SearchAPI.IMAGE_MODE) {
			sql = con.prepareStatement("SELECT * from get_images_for_disjunctive_search(?,?,?)");
		}
		if (sql != null) {
			sql.setArray(1, con.createArrayOf("text", expandedQueryTermsArr));
			sql.setArray(2, con.createArrayOf("text", requiredTermsArr));
			sql.setArray(3, con.createArrayOf("text", languages));
			sql.execute();
			ResultSet results = sql.getResultSet();
			List<String> allOriginalQueryTerms = new ArrayList<>();
			allOriginalQueryTerms.addAll(requiredTerms);
			allOriginalQueryTerms.addAll(searchTerms.get(QueryParser.NON_TILDA_TERMS));
			allOriginalQueryTerms.addAll(searchTerms.get(QueryParser.TILDA_TERMS));
			return processSearchQueryResultSet(con, results, allOriginalQueryTerms, expandedQueryTerms, k_limit,
					a_response, scoringMethod, searchMode);
		}
		throw new RuntimeException("You don't selected any search Mode");
	}

	private SearchResultResponse processSearchQueryResultSet(Connection con, ResultSet results,
			List<String> searchTerms, List<String> expandedQueryTerms, int k_limit, SearchResultResponse a_response,
			int scoringMethod, int searchMode) throws SQLException {

		HashMap<String, DBResponseDocument> resDocs = new HashMap<String, DBResponseDocument>();

		Map<String, Boolean> queryTermsMap = Utils.splitExpandedQuery(expandedQueryTerms);

		if (searchMode == SearchAPI.DOCUMENT_MODE) {
			while (results.next()) {
				String url = results.getString(1);
				String term = results.getString(2);
				float score_tfidf = results.getFloat(3);
				float score_okapi = results.getFloat(4);
				if (queryTermsMap.containsKey(term) && queryTermsMap.get(term) == false) { // term is not in base query
					score_tfidf /= 3; // weigh the expansion terms less than query terms by a factor of 3
					score_okapi /= 3;
				}
				if (!resDocs.containsKey(url))
					resDocs.put(url, new DBResponseDocument(url));
				DBResponseDocument doc = resDocs.get(url);
				doc.add_term(term, score_tfidf, score_okapi, score_okapi);
				doc.scoringMethod = scoringMethod;
			}
		} else if (searchMode == SearchAPI.IMAGE_MODE) {
			while (results.next()) {
				String url = results.getString(1);
				String url2 = results.getString(2);
				String term = results.getString(3);
				double score_exponential = results.getDouble(4);
				if (!resDocs.containsKey(url)) {
					DBResponseDocument r = new DBResponseDocument(url);
					r.url2 = url2;
					resDocs.put(url, r);
				}
				DBResponseDocument doc = resDocs.get(url);
				doc.add_term(term, 0, 0, score_exponential);
				doc.scoringMethod = scoringMethod;
			}
		} else {
			throw new RuntimeException("Not selected search mode");
		}
		results.close();
		con.close();

		DBResponseDocument queryDoc = new DBResponseDocument(null);
		// queryDoc.scoringMethod = scoringMethod;

		for (String t : searchTerms) {
			queryDoc.add_term(Utils.stemTerm(t), 1, 1, 1);
		}

		ArrayList<DBResponseDocument> sortedSet = new ArrayList<DBResponseDocument>();
		for (String key : resDocs.keySet()) {
			DBResponseDocument doc = resDocs.get(key);
			doc.calculateCosineSimilarity(queryDoc);
			sortedSet.add(doc);
		}

		Collections.sort(sortedSet, (d1, d2) -> -Double.compare(d1.getCosSimScore(), d2.getCosSimScore()));

		int rank = 1;
		if (a_response == null)
			a_response = new SearchResultResponse();

		if (searchMode == SearchAPI.DOCUMENT_MODE) {
			for (DBResponseDocument resDoc : sortedSet) {
				a_response.addSearchResultItem(rank++, resDoc.url, (float) resDoc.getCosSimScore(), createSnippet(
						getContentByURL(resDoc.getUrl()), searchTerms, getLanguageByURL(resDoc.getUrl())));
				if (rank > k_limit)
					break;
			}
		} else if (searchMode == SearchAPI.IMAGE_MODE) {
			for (DBResponseDocument resDoc : sortedSet) {
				a_response.addSearchResultItem(rank++, resDoc.url, resDoc.url2, (float) resDoc.getCosSimScore(),
						new Snippet("No snippet available", -1));
				if (rank > k_limit)
					break;
			}
		}

		return a_response;
	}

	public SearchResultResponse getStats(String query, SearchResultResponse a_response) throws SQLException {

		List<String> terms = QueryParser.getTermsWithoutQuotes_2(query);
		terms.addAll(QueryParser.getTermsInQuotes(query));
		String[] termsArr = (String[]) terms.toArray(new String[terms.size()]);

		Connection con = getConnection();
		PreparedStatement sql = con.prepareStatement("SELECT * from get_doc_frequencies(?)");
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
		//TODO: Change this to get number of terms in the whole collection. Maybe add a new column in the stats table.
		PreparedStatement sql = con.prepareStatement("SELECT cw from doc_stats_table");
		sql.execute();
		ResultSet results = sql.getResultSet();
		int retVal = 0;
		if (results.next())
			retVal = results.getInt(1);
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
		synchronized (DBHandler.class) {
			PreparedStatement stmtUpdateDoc = con.prepareStatement(
					"UPDATE documents SET crawled_on_date = CURRENT_DATE, language = ?, content = ? WHERE url LIKE ?");
			stmtUpdateDoc.setString(1, doc.getLanguage());
			stmtUpdateDoc.setString(2, doc.getContent());
			stmtUpdateDoc.setString(3, doc.getUrl().toString());
			stmtUpdateDoc.executeUpdate();
			stmtUpdateDoc.close();

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
				setNumberOfTerms.close();

				// Insert blank documents

				PreparedStatement stmtInsertBlankDocument = con.prepareStatement(
						"INSERT INTO documents (docid, url, crawled_on_date, language, page_rank, content) VALUES (DEFAULT, ?, NULL, NULL, NULL, NULL) ON CONFLICT DO NOTHING");
				for (URL url : doc.getLinks()) {
					stmtInsertBlankDocument.setString(1, url.toString());
					stmtInsertBlankDocument.executeUpdate();
				}
				stmtInsertBlankDocument.close();

				List<Integer> docKeys = new ArrayList<>();
				PreparedStatement stmtDocId = con.prepareStatement("SELECT docid FROM documents WHERE url LIKE ?");

				for (URL url : doc.getLinks()) {
					stmtDocId.setString(1, url.toString());
					stmtDocId.execute();
					ResultSet set = stmtDocId.getResultSet();
					if (set.next()) {
						docKeys.add(set.getInt(1));
					}
					set.close();
				}
				stmtDocId.close();

				// Insert outgoing links
				PreparedStatement stmtInsertLinks = con
						.prepareStatement("INSERT INTO links(from_docid, to_docid) VALUES (?,?)");
				for (int toDoc : docKeys) {
					stmtInsertLinks.setInt(1, docId);
					stmtInsertLinks.setInt(2, toDoc);
					stmtInsertLinks.addBatch();
				}
				stmtInsertLinks.executeBatch();
				stmtInsertLinks.close();
			}
			key.close();
			stmtgetDocId.close();
		}
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

	public void insertImageDataToDatabase(HTMLDocument doc, Connection con) throws SQLException {
		PreparedStatement stmtInsertImageFeature = con.prepareStatement(
				"INSERT INTO imagefeatures (imageurl, docid, term, ndist,score_exponential) VALUES (?,?,?,?,?)");
		PreparedStatement stmtgetDocId = con.prepareStatement("SELECT docid FROM documents WHERE url LIKE ?");
		stmtgetDocId.setString(1, doc.getUrl().toString());
		stmtgetDocId.execute();
		ResultSet rsDocId = stmtgetDocId.getResultSet();
		if (rsDocId.next()) {
			System.out.println("FOUND DOC");
			int docId = rsDocId.getInt(1);

			// Insert image features now
			for (Entry<MultiKey<? extends String>, Integer> e : doc.getTermDistances().entrySet()) {
				@SuppressWarnings("unchecked")
				MultiKey<String> mK = (MultiKey<String>) e.getKey();
				int tf = doc.getTermFrequencies().get((String) mK.getKey(1).toLowerCase());
				double score = (1 / (1 - Math.exp(-tf * e.getValue())));
				stmtInsertImageFeature.setString(1, (String) mK.getKey(0).toLowerCase());
				stmtInsertImageFeature.setInt(2, docId);
				stmtInsertImageFeature.setString(3, (String) mK.getKey(1).toLowerCase());
				stmtInsertImageFeature.setInt(4, e.getValue());
				stmtInsertImageFeature.setDouble(5, score);
				stmtInsertImageFeature.addBatch();
			}
			stmtInsertImageFeature.executeBatch();
		}
		rsDocId.close();
		stmtgetDocId.close();

		stmtInsertImageFeature.close();
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
		Connection con = this.getConnection();
		con.setAutoCommit(false);
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
			} else if (docColumn > lastDocColumn) {
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

		// System.out.println("PageRank computed");
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

	/**
	 * Returns the content of a document identified by the url
	 * 
	 * @param url
	 * @return Content of the document or NULL
	 * @throws SQLException
	 */
	public String getContentByURL(String url) throws SQLException {
		Connection con = getConnection();

		PreparedStatement ps = con.prepareStatement("SELECT content FROM documents WHERE url = ?");
		ps.setString(1, url);
		ps.execute();
		ResultSet rs = ps.getResultSet();
		if (!rs.next()) {
			return null;
		}

		String content = rs.getString(1);

		rs.close();
		ps.close();
		con.close();

		return content;
	}

	/**
	 * Returns the language of a document identified by the url
	 * 
	 * @param url
	 * @return
	 * @throws SQLException
	 */
	public String getLanguageByURL(String url) throws SQLException {
		Connection con = getConnection();

		PreparedStatement ps = con.prepareStatement("SELECT language FROM documents WHERE url = ?");
		ps.setString(1, url);
		ps.execute();
		ResultSet rs = ps.getResultSet();
		if (!rs.next()) {
			return null;
		}

		String language = rs.getString(1);

		rs.close();
		ps.close();
		con.close();

		return language;
	}

	/**
	 * Creates a preview snippet which has a maximum length of 32 terms
	 * 
	 * @param content     Content
	 * @param SearchTerms search terms to look for in the snippet
	 * @return Preview snippet with a maximum length of 32 terms
	 */
	private Snippet createSnippet(String content, List<String> terms, String language) {
		Stemmer stemmer = new Stemmer();

		TreeMap<String, Integer> termsFrequency;
		boolean withStemming = false;
		if (language.equals(HTMLDocument.Language.ENGLISH)) {
			// provide also the stemmed words
			withStemming = true;
			List<String> stemmedTerms = new ArrayList<String>();
			for (String t : terms) {
				stemmer.add(t.toCharArray(), t.length());
				stemmer.stem();
				stemmedTerms.add(stemmer.toString());
			}

			String stemmedContent = "";
			for (String t : content.split("\\s+")) {
				stemmer.add(t.toCharArray(), t.length());
				stemmer.stem();
				if (stemmedContent.equals("")) {
					stemmedContent += stemmer.toString();
				} else {
					stemmedContent += " " + stemmer.toString();
				}
			}

			termsFrequency = Utils.getOrderedTermsByFrequency(stemmedTerms, stemmedContent);
		} else {
			termsFrequency = Utils.getOrderedTermsByFrequency(terms, content);
		}

		String specialChars = "(\\>|\\/|&nbsp;|&bull;|&amp;|&#39;|\\,|\\:|\\;|\\[|\\]|\\{|\\}|\\||\\+|\\-|\\*|\\)|\\(|\\=|\\\"|\\|'|'|&|…|#|_)";
		String[] split = content.replaceAll(specialChars, " ").split("\\s+");
		List<String[]> s = new ArrayList<>();
		for (int x = 0; x < split.length; x += 100) {
			s.add(Arrays.copyOfRange(split, x,
					Math.min(x + 100, content.replaceAll(specialChars, " ").split("\\s+").length)));
			x -= 5;
		}

		List<Snippet> snippets = new ArrayList<Snippet>();
		for (String[] a : s) {
			SnippetGenerator sg = new SnippetGenerator(a, termsFrequency, 32, withStemming);
			snippets.add(sg.generateSnippet());
		}

		int amountTerms = 0;
		String completeSnippet = "";
		while (amountTerms < 32 && snippets.size() > 0) {
			Snippet next = Utils.getBestSnippet(snippets);
			if (next.getText().split(" ").length + amountTerms <= 32) {
				if (completeSnippet.equals("")) {
					completeSnippet += next.getText();
				} else {
					completeSnippet += " ... " + next.getText();
				}
				amountTerms += next.getText().split(" ").length;
			}
			snippets.remove(next);
		}

		String markedString = "";
		Set<String> lowerCaseTerms = new HashSet<String>();
		for (String t : terms) {
			lowerCaseTerms.add(t.toLowerCase());
		}

		if (language.equals(HTMLDocument.Language.ENGLISH)) {
			// provide also the stemmed words
			for (String t : terms) {
				stemmer.add(t.toLowerCase().toCharArray(), t.length());
				stemmer.stem();
				lowerCaseTerms.add(stemmer.toString().toLowerCase());
			}
		}

		for (String snip : completeSnippet.split("\\s+")) {
			// Do that only if document is english version
			String word = snip;
			if (language.equals(HTMLDocument.Language.ENGLISH)) {
				stemmer.add(snip.toCharArray(), snip.length());
				stemmer.stem();
				word = stemmer.toString();
			}

			if (lowerCaseTerms.contains(word.toLowerCase())) {
				if (markedString.equals("")) {
					markedString += Utils.wrapStringWithHtmlTag("b", snip);
				} else {
					markedString += " " + Utils.wrapStringWithHtmlTag("b", snip);
				}
			} else {
				if (markedString.equals("")) {
					markedString += snip;
				} else {
					markedString += " " + snip;
				}
			}
		}

		List<String> notExists = new ArrayList<String>();
		String markedLowerCaseString = markedString.toLowerCase();
		for (String term : lowerCaseTerms) {
			if (!markedLowerCaseString.contains(term)) {
				notExists.add(term);
			}
		}

		return new Snippet(markedString, -1, notExists);
	}

	public void callMakeShinglesFunction(int k) throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL makeShingles(?)");
		query.setInt(1, k);
		query.execute();
		query.close();
		con.close();
	}

	public void callJaccardShinglesFunction() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL computeJaccardValues()");
		query.execute();
		query.close();
		con.close();
	}

	public void callDoMinhashFunction() throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL do_minhash_table()");
		query.execute();
		query.close();
		con.close();
	}

	public void callJaccardMinhashFunction(int n) throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("CALL calculate_jaccard_minhash(?)");
		query.setInt(1, n);
		query.execute();
		query.close();
		con.close();
	}

	public Map<Integer, Float> getSimilarDocuments(int docid, float thresh) throws SQLException {
		Connection con = getConnection();
		PreparedStatement query = con.prepareStatement("SELECT * FROM get_similar_documents(?,?))");
		query.setInt(1, docid);
		query.setFloat(2, thresh);
		query.execute();
		ResultSet rs = query.getResultSet();
		Map<Integer, Float> simDocs = new HashMap<Integer, Float>();
		while (rs.next()) {
			int doc = rs.getInt(1);
			float jaccardSim = rs.getFloat(2);
			simDocs.put(doc, jaccardSim);
		}
		query.close();
		con.close();
		return simDocs;
	}

	public float getAverageJaccardError() throws SQLException {
		Connection con = getConnection();
		String query = "SELECT AVG(err) FROM " + "(SELECT ABS(actual.jaccardVal-approx.jaccardVal) err "
				+ "FROM jaccard_values actual " + "JOIN jaccard_values_minhash approx "
				+ "ON (actual.d1=approx.d1 AND actual.d2=approx.d2) " + ") a;";
		PreparedStatement stmt = con.prepareStatement(query);
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		float error = 0;
		if (rs.next())
			error = rs.getFloat(1);
		stmt.close();
		con.close();
		return error;
	}

	public float getJaccardQuartileError(float quartile) throws SQLException {
		Connection con = getConnection();
		String query = "SELECT percentile_disc(?) WITHIN GROUP(ORDER BY err) FROM "
				+ "(SELECT ABS(actual.jaccardVal-approx.jaccardVal) err " + "FROM jaccard_values actual "
				+ "JOIN jaccard_values_minhash approx " + "ON (actual.d1=approx.d1 AND actual.d2=approx.d2) " + ") a;";
		PreparedStatement stmt = con.prepareStatement(query);
		stmt.setFloat(1, quartile);
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		float error = 0;
		if (rs.next())
			error = rs.getFloat(1);
		stmt.close();
		con.close();
		return error;
	}

	public boolean insertAd(String url, String imageURL, String description, String[] ngrams, double pricePerClick, double totalBudget) throws SQLException {
		Connection con = getConnection();
		PreparedStatement stmt = con.prepareStatement("INSERT INTO ads(url,imageURL,description,pricePerClick,totalBudget,ngrams) VALUES (?,?,?,?,?,?);");
		
		stmt.setString(1,url);
		stmt.setString(2, imageURL);
		stmt.setString(3, description);
		stmt.setDouble(4, pricePerClick);
		stmt.setDouble(5, totalBudget);
		stmt.setArray(6, con.createArrayOf("TEXT", ngrams));
		stmt.execute();
		
		stmt.close();
		con.close();
		
		return true;
	}
	
	public List<Ad> getAllAds() throws SQLException{
		Connection con = getConnection();
		Statement stmt = con.createStatement();
		ResultSet r = stmt.executeQuery("SELECT * FROM ads WHERE (totalBudget - pricePerClick) >= 0.00");
		List<Ad> ads = new ArrayList<Ad>();
		while(r.next()) {
			ads.add(new Ad(r.getInt(1), r.getString(3), r.getString(7), 0, r.getString(2), (String[]) r.getArray(6).getArray()));
		}
		return ads;
	}
	
	public void incrementAdClicks(int id) throws SQLException {
		Connection con = getConnection();
		PreparedStatement stmt = con.prepareStatement("UPDATE ads SET clickCount = clickCount+1, totalBudget = totalBudget - pricePerClick  WHERE id = ?");
		stmt.setInt(1, id);
		stmt.execute();
		stmt.close();
		con.close();
	}
	
}