package com.example.main.backend;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class CrawlerRunnable implements Runnable {

	private URL urlToCrawl;

	private int depth;

	public CrawlerRunnable(URL urlToCrawl, int parentDepth) {
		this.urlToCrawl = urlToCrawl;
		this.depth = parentDepth + 1;
	}

	@Override
	public void run() {
		Connection con = null;
		try {

			con = DriverManager.getConnection("jdbc:postgresql:project", "postgres", "postgres");

			HTMLDocument doc = Indexer.index(urlToCrawl);

			con.setAutoCommit(false);

			insertDocDataToDatabase(doc, con);
			insertURLSToQueue(doc.getLinks(), depth, con);
			insertURLToVisited(doc.getUrl(), con);

			con.commit();

		} catch (IOException | URISyntaxException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void insertDocDataToDatabase(HTMLDocument doc, Connection con) throws SQLException, MalformedURLException {

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

	private void insertURLToVisited(URL url, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmt = con
				.prepareStatement("INSERT INTO crawlerVisitedPages (url, last_visited) VALUES (?, CURRENT_DATE)");
		stmt.setString(1, url.toString());
		stmt.execute();
		stmt.close();
	}

	private void insertURLSToQueue(Set<URL> urls, int currentDepth, Connection con) throws SQLException {

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
