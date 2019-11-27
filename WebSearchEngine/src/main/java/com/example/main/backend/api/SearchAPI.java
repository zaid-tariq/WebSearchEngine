package com.example.main.backend.api;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.api.responseObjects.SearchResultResponse;

public class SearchAPI {

	class Query {
		public String query;
		public URL site;

		public Query() {

		}

		public Query(String q, URL u) {
			this.query = q;
			this.site = u;
		}
	}

	public SearchAPI() {
	}

	public Query resolveSiteOperator(String a_query) {

		Query q = new Query();
		a_query = a_query.trim();

		// That means, that a side operator is included in that query
		int indexOperator = a_query.indexOf("site:");
		if (indexOperator == -1) {
			q.query = a_query;
			System.out.println("BRANCH 1");
		} else if (indexOperator > 0) {
			// Split by site operator --> this is the easy way
			String[] tokens = a_query.split("site:");
			if (tokens.length == 2) {
				q.query = tokens[0];
				String site = tokens[1];
				try {
					URL url = new URL(site.trim());
					q.site = url;
					System.out.println("BRANCH");
				} catch (Exception ex) {
					System.out.println("Not a valid site. Operator ignored.");
				}
			}
			System.out.println("BRANCH 2");
		} else if (indexOperator == 0) {
			String[] tokens = a_query.split(" ");
			// Check if site operator and url are combined in one token
			String site = "";
			if (tokens[0].trim().length() > 5) {
				// Yes they are combined
				site = tokens[0].trim().substring(5);
			} else {
				// No there was a space between it
				site = tokens[1].trim();
			}
			System.out.println("BRANCH 3");
			// convert to url
			try {
				URL url = new URL(site);
				q.site = url;
			} catch (Exception ex) {
				System.out.println("Not a valid site. Operator ignored.");
			}
		}
		return q;
	}

	public SearchResultResponse searchAPIconjunctive(String a_query, int limit) {

		Query q = resolveSiteOperator(a_query);
		Connection con = null;
		SearchResultResponse res = new SearchResultResponse(q.query, limit);

		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			res = handler.searchConjunctiveQuery(con, q.query, limit, res);
			int cw = handler.getCollectionSize(con);
			res.setCollectionSize(cw);
			res = handler.getStats(con, q.query, res);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
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
		res.filterResultsWithSite(q.site);
		return res;
	}

	public SearchResultResponse searchAPIdisjunctive(String a_query, int limit) {

		Query q = resolveSiteOperator(a_query);
		SearchResultResponse res = new SearchResultResponse(q.query, limit);
		Connection con = null;

		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			res = handler.searchDisjunctiveQuery(con, q.query, limit, res);
			int cw = handler.getCollectionSize(con);
			res.setCollectionSize(cw);
			res = handler.getStats(con, q.query, res);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
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
		res.filterResultsWithSite(q.site);
		return res;
	}

	public void updateScores() {
		Connection con = null;
		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			handler.computeTfIdf(con);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
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
}
