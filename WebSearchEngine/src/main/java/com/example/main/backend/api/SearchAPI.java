package com.example.main.backend.api;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.api.responseObjects.SearchResultResponse;

public class SearchAPI {

	class Query{
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
		
		String[] tokens = a_query.split("site:");
		Query q = new Query();
		
		if(tokens.length > 1) {
			q.query = tokens[0];
			String site = tokens[1];
			try {
				URL url = new URL(site.trim());
				q.site = url;
			}
			catch(Exception ex) {
				System.out.println("Not a valid site. Operator ignored.");
			}			
		}
		else q.query = a_query;
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
		}
		catch( SQLException ex) {
			ex.printStackTrace();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if(con != null) {
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
		}
		catch( SQLException ex) {
			ex.printStackTrace();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if(con != null) {
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
		}
		catch( SQLException ex) {
			ex.printStackTrace();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if(con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

