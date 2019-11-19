package backend;

import java.sql.*;

public class DBHandler {

	public void computeTfIdf(Connection a_conn) throws SQLException {
		PreparedStatement query = a_conn.prepareStatement("CALL update_tf_idf_scores()");
		query.execute();
	}

	public void searchConjunctiveQuery(Connection a_conn, String a_searchQuery, int a_k) throws SQLException {
		processSearchResults("SELECT * from conjunctive_search(?, ?)", a_searchQuery, a_k, a_conn);
	}

	public void searchDisjunctiveQuery(Connection a_conn, String a_searchQuery, int a_k) throws SQLException {
		processSearchResults("SELECT * from disjunctive_search(?, ?)", a_searchQuery, a_k, a_conn);
	}

	private void processSearchResults(String a_sqlQuery, String a_searchQuery, int a_k, Connection a_conn)
			throws SQLException {
		PreparedStatement query = a_conn.prepareStatement(a_sqlQuery);
		query.setString(1, a_searchQuery);
		query.setInt(2, a_k);
		query.execute();
		ResultSet results = query.getResultSet();
		while (results.next()) {
			System.out.println(results.getString(1) + "," + results.getFloat(2));
		}

	}
}