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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.main.backend.api.responseObjects.SearchResultResponse;

@Repository
public class DBHandler {

	@Autowired
	public DataSource dataSource;

	public Connection getConnection() throws SQLException {
		return this.dataSource.getConnection();
	}
	
	public void computeTfIdf() throws SQLException {
		Connection conn = this.getConnection();
		PreparedStatement query = conn.prepareStatement("CALL update_tf_idf_scores()");
		query.execute();
	}

	public SearchResultResponse searchConjunctiveQuery(String query, int a_k, SearchResultResponse a_response)
			throws SQLException {

		List<String> searchTerms = getTermsInQuotes(query);
		String[] searchTermsArr = getTermsInQuotes(query).toArray(new String[searchTerms.size()]);

		Connection conn = this.getConnection();
		PreparedStatement sql = conn.prepareStatement("SELECT * from conjunctive_search(?, ?)");
		sql.setArray(1, conn.createArrayOf("text", searchTermsArr));
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
		return a_response;
	}

	public SearchResultResponse searchDisjunctiveQuery(String query, int a_k, SearchResultResponse a_response)
			throws SQLException {

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

		Connection conn = this.getConnection();
		PreparedStatement sql = conn.prepareStatement("SELECT * from disjunctive_search(?,?,?)");
		sql.setArray(1, conn.createArrayOf("text", searchTermsArr));
		sql.setArray(2, conn.createArrayOf("text", requiredTermsArr));
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
		return a_response;
	}

	public SearchResultResponse getStats(String query, SearchResultResponse a_response) throws SQLException {

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

		Connection conn = this.getConnection();
		PreparedStatement sql = conn.prepareStatement("SELECT * from get_term_frequencies(?)");
		sql.setArray(1, conn.createArrayOf("text", termsArr));
		sql.execute();
		ResultSet results = sql.getResultSet();

		if (a_response == null)
			a_response = new SearchResultResponse();

		while (results.next()) {
			String term = results.getString(1);
			int df = results.getInt(2);
			a_response.addStat(df, term);
		}

		return a_response;
	}

	public int getCollectionSize() throws SQLException {
		Connection conn = this.getConnection();
		PreparedStatement sql = conn.prepareStatement("SELECT COUNT(docid) from documents");
		sql.execute();
		ResultSet results = sql.getResultSet();
		results.next();
		return results.getInt(1);
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
	 * 
	 * @param maximumDepth
	 * @param maximumNumberOfDocs
	 * @param crawledDocuments
	 * @param leaveDomain
	 * @param parallelism
	 * 
	 * @return id of the save state, -1 if there was an error
	 */
	public int cancel(int maximumDepth, int maximumNumberOfDocs, int crawledDocuments, boolean leaveDomain,
			int parallelism) {
		try {
			PreparedStatement ps = this.getConnection().prepareStatement(
					"INSERT INTO crawlerState (id, maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism) VALUES (DEFAULT,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, maximumDepth);
			ps.setInt(2, maximumNumberOfDocs);
			ps.setInt(3, crawledDocuments);
			ps.setBoolean(4, leaveDomain);
			ps.setInt(5, parallelism);

			ps.executeUpdate();

			ResultSet key = ps.getGeneratedKeys();
			if (key.next()) {
				ps.close();
				return key.getInt(1);
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return -1;
	}

	/**
	 * Gets the next URL from the database
	 * 
	 * @param stmtNextURL
	 * 
	 * @return null if there are no more URLs in the database, otherwise the URL and
	 *         the depth of that URL in the hierarchy
	 * @throws SQLException
	 * @throws URISyntaxException
	 */
	public Object[] getNextURL(PreparedStatement stmtNextURL) throws SQLException, URISyntaxException {
		stmtNextURL.execute();
		ResultSet res = stmtNextURL.getResultSet();
		if (res.next()) {
			PreparedStatement stmnt = this.getConnection()
					.prepareStatement("DELETE FROM crawlerQueue WHERE id = ?");
			stmnt.setInt(1, res.getInt(1));
			stmnt.execute();
			stmnt.close();
			try {
				return new Object[] { new URL(res.getString(2)), res.getInt(3) };
			} catch (MalformedURLException e) {
				return null;
			}
		}

		return null;
	}

	/**
	 * Inserts the provided URLs into the database
	 * 
	 * @param urls URLs to insert
	 * @param con  Connection to the database
	 * @throws SQLException
	 * @throws MalformedURLException
	 */
	public void queueURLs(Set<URL> urls) throws SQLException, MalformedURLException {
		Connection con = this.getConnection();
		PreparedStatement stmtQueueURLs = con
				.prepareStatement("INSERT INTO crawlerQueue(id, url, current_depth) VALUES (DEFAULT, ?, ?)");

		for (URL url : urls) {
			stmtQueueURLs.setString(1, url.toString());
			stmtQueueURLs.setInt(2, 0);
			stmtQueueURLs.addBatch();
		}

		stmtQueueURLs.executeBatch();

		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO documents (docid, url,crawled_on_date, language) VALUES (DEFAULT,?,NULL,NULL)");

		for (URL url : urls) {
			stmt.setString(1, url.toString());
			stmt.addBatch();
		}

		stmt.executeBatch();
	}

	public Crawler restoreCrawler() throws SQLException {
		Crawler crawler = null;

		PreparedStatement ps = this.getConnection().prepareStatement(
				"SELECT id, maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism FROM crawlerState WHERE id = ?");
		ps.execute();
		ResultSet res = ps.getResultSet();
		if (res.next()) {
			// Only if there is a valid configuration stored in the database
			crawler = new Crawler();
			crawler.init(new HashSet<URL>(), res.getInt(2), res.getInt(3), res.getBoolean(5), res.getInt(6));
			crawler.setCrawledDocuments(res.getInt(4));
		}
		ps.close();
		return crawler;
	}

	public PreparedStatement getPreparedStatement(String sql) throws SQLException {

		return this.getConnection().prepareStatement(sql);
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
					"INSERT INTO documents (docid, url, crawled_on_date, language) VALUES (DEFAULT, ?, NULL, NULL) ON CONFLICT DO NOTHING");
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

		PreparedStatement stmtgetDocId = con.prepareStatement("SELECT docid FROM documents WHERE url LIKE ?");

		PreparedStatement stmt = con.prepareStatement("INSERT INTO crawlerQueue (url, current_depth) VALUES (?,?)");
		for (URL url : urls) {
			stmtgetDocId.setString(1, url.toString());
			stmtgetDocId.execute();
			ResultSet s = stmtgetDocId.getResultSet();
			if (s.next()) {
				stmt.setString(1, url.toString());
				stmt.setInt(2, currentDepth);
				stmt.addBatch();
			}

		}
		stmt.executeBatch();
		stmt.close();
	}

}