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
		try {

			// TODO:create database connection
			Connection con = DriverManager.getConnection("jdbc:postgresql:websearch","postgres","postgres");

			HTMLDocument doc = Indexer.index(urlToCrawl);

			con.setAutoCommit(false);

			insertDocDataToDatabase(doc, con);
			insertURLToVisited(doc.getUrl(), con);

			con.commit();
			con.close();

		} catch (IOException | URISyntaxException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void insertDocDataToDatabase(HTMLDocument doc, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmtInsertDoc = con.prepareStatement(
				"INSERT INTO documents (url,crawled_on_date, language) VALUES (?,CURRENT_DATE,?)",
				Statement.RETURN_GENERATED_KEYS);
		stmtInsertDoc.setString(0, doc.getUrl().toURL().toString());
		stmtInsertDoc.setString(2, doc.getLanguage());
		stmtInsertDoc.executeUpdate();

		ResultSet key = stmtInsertDoc.getGeneratedKeys();
		key.next();
		int docId = key.getInt(0);
		stmtInsertDoc.close();

		PreparedStatement stmtInsertFeature = con
				.prepareStatement("INSERT INTO features (docid, term, term_frequency) VALUES (?,?,?)");
		for (Entry<String, Integer> e : doc.getTermFrequencies().entrySet()) {
			stmtInsertFeature.setInt(0, docId);
			stmtInsertFeature.setString(1, e.getKey());
			stmtInsertFeature.setInt(2, e.getValue());
			stmtInsertFeature.addBatch();
		}
		stmtInsertFeature.executeBatch();
		stmtInsertFeature.close();

		// TODO: insert links
		// PreparedStatement stmtInsertLinks = con.prepareStatement("INSERT INTO
		// documents (url, crawled_on_date, language)")
	}

	private void insertURLToVisited(URI url, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmt = con
				.prepareStatement("INSERT INTO crawlerVisitedPages (url, last_visited) VALUES (?, CURRENT_DATE)");
		stmt.setString(0, url.toURL().toString());
		stmt.execute();
		stmt.close();
	}
}
