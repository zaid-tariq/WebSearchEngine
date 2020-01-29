package com.example.main.backend.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.responseObjects.MetaSearchResponseConfigObject;
import com.example.main.backend.api.responseObjects.MetaSearchResultItem;
import com.example.main.backend.api.responseObjects.MetaSearchResultResponse;
import com.example.main.backend.utils.Utils;

@Service
public class MetaSearchEngine {
	
	@Autowired
	DBHandler db;
	
	private RestTemplate restTemplate;
	private ExecutorService exs;
	
	@Autowired
	public MetaSearchEngine(RestTemplateBuilder builder) {
		
		this.restTemplate = builder.build();
		exs = Executors.newCachedThreadPool();
	}
	
	public MetaSearchResponseConfigObject dispatch_config_action(String engineUrl, String action) {
		
		MetaSearchResponseConfigObject response = new MetaSearchResponseConfigObject();
		
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
		
		//TODO: Check that url points to a valid website
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
	
	
	void loadSearchEnginesFromDB(MetaSearchResponseConfigObject a_response) throws SQLException {
		
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
	
	List<String> getEnabledSearchEnginesFromDB() throws SQLException {
		
		List<String> urls = new ArrayList<String>();
		Connection con = db.getConnection();
		PreparedStatement query = con.prepareStatement("SELECT engine_url FROM metasearch_config WHERE enabled=true");
		query.execute();
		ResultSet rs = query.getResultSet();
		while(rs.next()) {
			String engineURL = rs.getString(1);
			urls.add(engineURL);
		}
		query.close();	
		closeConnection(con);
		return urls;
	}
	
	public MetaSearchResultResponse querySearchEngineAPI(String a_engineUrl, String a_query, int k) {

		//check engine Url is correct. But, this should be done while saving the URL to the DB. Just ping the engine and if the request is 200, save.
		
		a_engineUrl += "?query="+Utils.formatQueryStringForGetUrlRequest(a_query)+"&k="+k;
		return this.restTemplate.getForObject(a_engineUrl, MetaSearchResultResponse.class);
	}
	
	MetaSearchResultResponse combineResults(Map<String, Future<MetaSearchResultResponse>> engineResults) {
		
		//TODO: Use the algoirthm provided in the sheet
		
		MetaSearchResultResponse combinedRes = new MetaSearchResultResponse();
		
		for(Entry<String, Future<MetaSearchResultResponse>> entry: engineResults.entrySet()) {
			
			try {
				String engineUrl = entry.getKey();
				MetaSearchResultResponse res;
				res = entry.getValue().get();
				for(MetaSearchResultItem resLink:res.getResultList()) {
					resLink.setSource(engineUrl);
					combinedRes.resultList.add(resLink);
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} 
		}
		return combinedRes;
	}

	public MetaSearchResultResponse getSearchResults(String query) throws SQLException{
		
		//conundrum:
		// What should k be? Should it be calculated AFTER I get results from all remote engines and then apply their respective percentages to them?
		//Or should I calculate this BEFORE I send requests to each engine? As in, calculate the number of of results I want from each engine using their quotas
		//Going with the second approach
		
		List<String> engineUrls = getEnabledSearchEnginesFromDB();
		Map<String, Future<MetaSearchResultResponse>> engineResults = new HashMap<String, Future<MetaSearchResultResponse>>();
		for(String url: engineUrls) {
			Future<MetaSearchResultResponse> thread = exs.submit(()->{
				return querySearchEngineAPI(url, query, 20); //calculate K based on engine's percentage
			});		
			engineResults.put(url, thread);
		}
		
		return combineResults(engineResults);
	}

}
