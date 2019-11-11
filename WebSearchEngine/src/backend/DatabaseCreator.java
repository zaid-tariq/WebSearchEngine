package backend;

import java.sql.*;

public class DatabaseCreator {

	private Connection connection;

	public void create() {
		try {
			connection = DriverManager.getConnection("");

			// Create tables by DDL statements
			createDocumentsTable(connection);
			createFeatureTable(connection);
			createLinksTable(connection);

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
				"CREATE TABLE features (docid INT REFERENCES documents(docid), term TEXT, term_frequency INT)");
		statement.execute();
		statement.close();
	}

	private void createDocumentsTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE documents (docid INT PRIMARY KEY , url TEXT NOT NULL , crawled_on_date DATE, language TEXT)");
		statement.execute();
		statement.close();
	}

	private void createLinksTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE links (from_docid INT REFERENCES documents(docid), to_docid INT REFERENCES documents(docid))");
		statement.execute();
		statement.close();
	}
}