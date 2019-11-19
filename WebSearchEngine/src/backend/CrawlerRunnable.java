package backend;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map.Entry;

public class CrawlerRunnable implements Runnable {

	private URI urlToCrawl;

	public CrawlerRunnable(URI urlToCrawl) {
		this.urlToCrawl = urlToCrawl;
	}

	@Override
	public void run() {
		System.out.println("thread running");
		Connection con = null;
		try {

			// TODO:create database connection
			con = DriverManager.getConnection("jdbc:postgresql:project","app","pass");
			System.out.println("conn acquired");

			HTMLDocument doc = Indexer.index(urlToCrawl);
			System.out.println("please print + "+doc.getUrl());

			con.setAutoCommit(false);

			insertDocDataToDatabase(doc, con);
			insertURLToVisited(doc.getUrl(), con);

			con.commit();
			

		} catch (IOException | URISyntaxException | SQLException e) {
			e.printStackTrace();
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void insertDocDataToDatabase(HTMLDocument doc, Connection con) throws SQLException, MalformedURLException {
		
		PreparedStatement stmtInsertDoc = con.prepareStatement(
				"INSERT INTO documents (docid, url,crawled_on_date, language) VALUES (5, ?,CURRENT_DATE,?)",
				Statement.RETURN_GENERATED_KEYS);
		stmtInsertDoc.setString(1, doc.getUrl().toString());
		stmtInsertDoc.setString(2, doc.getLanguage());
		stmtInsertDoc.executeUpdate();

		ResultSet key = stmtInsertDoc.getGeneratedKeys();
		if(key.next()) {
		int docId = key.getInt(1);
		stmtInsertDoc.close();
		
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
	}

		// TODO: insert links
		// PreparedStatement stmtInsertLinks = con.prepareStatement("INSERT INTO
		// documents (url, crawled_on_date, language)")
	}

	private void insertURLToVisited(URI url, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmt = con
				.prepareStatement("INSERT INTO crawlerVisitedPages (url, last_visited) VALUES (?, CURRENT_DATE)");
		stmt.setString(1, url.toString());
		stmt.execute();
		stmt.close();
	}
}
