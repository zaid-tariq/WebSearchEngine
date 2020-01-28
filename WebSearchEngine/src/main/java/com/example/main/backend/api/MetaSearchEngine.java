package com.example.main.backend.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.responseObjects.MetaSearchResponseObject;

@Component
public class MetaSearchEngine {
	
	@Autowired
	DBHandler db;
	
	public MetaSearchResponseObject dispatch_config_action(String engineUrl, String action) {
		
		MetaSearchResponseObject response = new MetaSearchResponseObject();
		
		try {
			switch(action) {
			
				case "add":
					addNewSearchEngine(engineUrl);
					break;
				case "delete":
					deleteSearchEngine(engineUrl);
					break;
				case "enable":
					enableSearchEngine(engineUrl);
					break;
				case "disable":
					disableSearchEngine(engineUrl);
					break;
				case "load":
					loadSearchEnginesFromDB(response);
			}
			response.setRequestStatus(true);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			response.setRequestStatus(false);
		}
		
		return response;
	}
	
	void closeConnection(Connection con) {
		if(con != null)
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	boolean addNewSearchEngine(String url) throws Exception {
		
		Connection con = db.getConnection();
		PreparedStatement query = con.prepareStatement("INSERT INTO metasearch_config VALUES(?, true)");
		query.setString(1, url);
		int count = query.executeUpdate();
		query.close();
		closeConnection(con);
		if(count < 1)
			throw new Exception("Matching row not found in DB");
		return true;
	}
	
	boolean disableSearchEngine(String url) throws Exception {
		
		Connection con = db.getConnection();
		PreparedStatement query = con.prepareStatement("UPDATE metasearch_config SET enabled=false WHERE engine_url=?");
		query.setString(1, url);
		int count = query.executeUpdate();
		query.close();
		closeConnection(con);
		if(count < 1)
			throw new Exception("Matching row not found in DB");
		return true;
	}
	
	
	boolean enableSearchEngine(String url) throws Exception {
		
		Connection con = db.getConnection();
		PreparedStatement query = con.prepareStatement("UPDATE metasearch_config SET enabled=true WHERE engine_url=?");
		query.setString(1, url);
		int count = query.executeUpdate();
		query.close();
		closeConnection(con);
		if(count < 1)
			throw new Exception("Matching row not found in DB");
		return true;
	}
	
	
	boolean deleteSearchEngine(String url) throws Exception {
		
		Connection con = db.getConnection();
		PreparedStatement query = con.prepareStatement("DELETE FROM metasearch_config WHERE engine_url=?");
		query.setString(1, url);
		int count = query.executeUpdate();
		query.close();
		closeConnection(con);
		if(count < 1)
			throw new Exception("Matching row not found in DB");
		return true;
	}
	
	
	void loadSearchEnginesFromDB(MetaSearchResponseObject a_response) throws SQLException {
		
		Connection con = db.getConnection();
		PreparedStatement query = con.prepareStatement("SELECT engine_url, enabled FROM metasearch_config");
		query.execute();
		ResultSet rs = query.getResultSet();
		while(rs.next()) {
			String engineURL = rs.getString(1);
			boolean enabled = rs.getBoolean(2);
			a_response.addEngineUrl(engineURL, enabled);
		}
		query.close();	
		closeConnection(con);
	}

}
