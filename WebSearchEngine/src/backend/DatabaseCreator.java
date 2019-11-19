package backend;

import java.sql.*;

public class DatabaseCreator {

	private Connection connection;

	public void create() {
		try {
			connection = DriverManager.getConnection("jdbc:postgresql:websearch","postgres","postgres");

			// Create tables by DDL statements
			createDocumentsTable(connection);
			createFeatureTable(connection);
			createLinksTable(connection);

			createCrawlerStateTable(connection);
			createCrawlerQueueTable(connection);
			createCrawlerVisitedPagesTable(connection);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void createFeatureTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS features (docid INT REFERENCES documents(docid), term TEXT, term_frequency INT, tf_idf FLOAT)");
		statement.execute();
		statement.close();
	}

	private void createDocumentsTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS documents (docid SERIAL PRIMARY KEY, url TEXT NOT NULL , crawled_on_date DATE, language TEXT)");
		statement.execute();
		statement.close();
	}

	private void createLinksTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS links (from_docid INT REFERENCES documents(docid), to_docid INT REFERENCES documents(docid))");
		statement.execute();
		statement.close();
	}

	private void createCrawlerQueueTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS crawlerQueue (id SERIAL PRIMARY KEY, url TEXT NOT NULL, current_depth INT NOT NULL");
		statement.execute();
		statement.close();
	}

	private void createCrawlerVisitedPagesTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS crawlerVisitedPages (url TEXT NOT NULL, last_visited DATE)");
		statement.execute();
		statement.close();
	}

	private void createCrawlerStateTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS crawlerState (id INT PRIMARY KEY, maximum_depth INT NOT NULL, maximum_docs INT NOT NULL, crawled_docs INT NOT NULL, leave_domain BOOLEAN, parallelism INT NOT NULL)");
		statement.execute();
		statement.close();
	}
}