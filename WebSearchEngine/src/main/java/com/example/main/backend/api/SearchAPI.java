package com.example.main.backend.api;

import java.sql.Connection;
import java.sql.SQLException;

import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.api.responseObjects.SearchResultResponse;

public class SearchAPI {

	
	public SearchAPI() {
	}
	
	public SearchResultResponse searchAPIconjunctive(String query, int limit) {
		
		Connection con = null;
		SearchResultResponse res = new SearchResultResponse(query, limit);
		
		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			res = handler.searchConjunctiveQuery(con, query, limit, res);
			int cw = handler.getCollectionSize(con);
			res.setCollectionSize(cw);
			res = handler.getStats(con, query, res);
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
		
		return res;
	}
	
	public SearchResultResponse searchAPIdisjunctive(String query, int limit) {
		
		SearchResultResponse res = new SearchResultResponse(query, limit);
		Connection con = null;
		
		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			res = handler.searchDisjunctiveQuery(con, query, limit, res);
			int cw = handler.getCollectionSize(con);
			res.setCollectionSize(cw);
			res = handler.getStats(con, query, res);
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
		
		return res;
	}
}

