package com.example.main.backend.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import com.example.main.backend.api.responseObjects.StatItem;
import com.example.main.backend.dao.MetaSearchEngineStats;
import com.example.main.backend.dao.MetaSearchEngineStats.TermStats;
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
		
		//Check that url points to a valid website?
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
	
	
	
	List<MetaSearchEngineStats> getRelevantSearchEnginesFromDB(String query) throws SQLException {

		Connection con = db.getConnection();
		PreparedStatement stmt = con.prepareStatement("SELECT engine_url, term, T_score, I_score FROM getRelevantSearchEngines(?)");

		stmt.setString(1, query);
		stmt.execute();
		ResultSet rs = stmt.getResultSet();
		List<MetaSearchEngineStats> engines = new ArrayList<MetaSearchEngineStats>();
		MetaSearchEngineStats currEngine = null;
		while(rs.next()) {
			String engineURL = rs.getString(1);
			if(currEngine == null || currEngine.getUrl() != engineURL) {
				currEngine = new MetaSearchEngineStats();
				currEngine.setUrl(engineURL);
				engines.add(currEngine);
			}
			String term = rs.getString(2);
			Float t_score = rs.getFloat(3);
			Float i_score = rs.getFloat(4);
			currEngine.addTerm(term, t_score, i_score);
		}
		stmt.close();	
		closeConnection(con);
		
		for(MetaSearchEngineStats engine:engines)
			engine.computeCoriScore();
		
		Collections.sort(engines, (e1, e2) -> -Double.compare(e1.getCori(), e2.getCori()));
		
		return engines;
	}
	
	public MetaSearchResultResponse querySearchEngineAPI(String a_engineUrl, String a_query, int k) {

		a_engineUrl += "?query="+Utils.formatQueryStringForGetUrlRequest(a_query)+"&k="+k;
		return this.restTemplate.getForObject(a_engineUrl, MetaSearchResultResponse.class);
	}
	
	double computeNormalizedDocScore(Double R_dash, Double D) {
		double D_dash = (D + (0.4 * D * R_dash) ) / 1.4;
		return D_dash;
	}
	
	MetaSearchResultResponse combineResults(List<MetaSearchEngineStats> engines) {
		
		MetaSearchResultResponse combinedRes = new MetaSearchResultResponse();
		
		for(MetaSearchEngineStats engine: engines) {
			
			try {
				if(engine.getQueryResult() != null) {
					MetaSearchResultResponse searchResultsObj = engine.getQueryResult().get();
					Double r_dash_score = engine.compute_r_dash_score();
					for(MetaSearchResultItem searchResult:searchResultsObj.getResultList()){
						double normalized_score = computeNormalizedDocScore(r_dash_score, (double) searchResult.getScore());
						searchResult.setScore((float) normalized_score);
						searchResult.setSource(engine.getUrl());
						combinedRes.resultList.add(searchResult);
					}
					//updating stats 
					engine.setCw(searchResultsObj.getCw());
					for(StatItem queryStat : searchResultsObj.getStat()) {
						int df = queryStat.getDf();
						String term = queryStat.getTerm();
						engine.upateTermStat(term, df);
					}
				}
				else {
					//Optimization ideas for later:
					//-> add the query terms to the not-collected-selected engine and set their df to 0 so that the interval function will read them and get the stats from the search engine when it runs
					//-> in the interval function, maybe, also randomly select some engine to update their existing stats. Maybe.
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} 
		}
		
		exs.submit(()->{
			updateStatsInDB(engines);
			updateCoriStats();
		});	
		
		Collections.sort(combinedRes.getResultList(), (d1, d2) -> -Double.compare(d1.getScore(), d2.getScore()));
		
		return combinedRes;
	}
	
	
	public void updateCoriStats() {
		//should run this in separate thread that runs after some interval?
		try {
			Connection con = db.getConnection();
			PreparedStatement stmt = con.prepareStatement("CALL updateCoriStats()");
			stmt.execute();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void updateStatsInDB(List<MetaSearchEngineStats> engines) {
		
		try {
			Connection conn = db.getConnection();
			PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO metasearch_cori (engine_url, term, df) VALUES(?,?,?) "
					+ "ON CONFLICT (engine_url, term) DO UPDATE SET df=? WHERE metasearch_cori.engine_url=? AND metasearch_cori.term=? ");
			PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO metasearch_collection_stats (engine_url, cw) VALUES(?,?) "
					+ "ON CONFLICT (engine_url) DO UPDATE SET cw=? WHERE metasearch_collection_stats.engine_url=?");
			for(MetaSearchEngineStats engine: engines) {
				engine.getTerms().addAll(engine.getUnknownTerms());
				for(TermStats termStat : engine.getTerms()) {
					if(termStat.getTerm() == null)
						continue;
					stmt.setString(1, engine.getUrl());
					stmt.setString(2, termStat.getTerm());
					stmt.setInt(3, termStat.getDf());
					stmt.setInt(4, termStat.getDf());
					stmt.setString(5, engine.getUrl());
					stmt.setString(6, termStat.getTerm());
					stmt.addBatch();
				}

				stmt2.setString(1, engine.getUrl());
				stmt2.setInt(2, engine.getCw());
				stmt2.setInt(3, engine.getCw());
				stmt2.setString(4, engine.getUrl());
				stmt2.addBatch();
			}
			stmt2.executeBatch();
			stmt.executeBatch();
			stmt.close();
			stmt2.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	

	public MetaSearchResultResponse getSearchResults(String query) throws SQLException{

		List<MetaSearchEngineStats> engines = getRelevantSearchEnginesFromDB(query);
		int n = 10; //send the query only to the top 10 relevant engines
		for(int i = 0; i < n && i < engines.size(); i++){
			MetaSearchEngineStats engine = engines.get(i);
			Future<MetaSearchResultResponse> queryResult = exs.submit(()->{
				return querySearchEngineAPI(engine.getUrl(), query, 20); //Maybe calculate K based on engine's cori score?
			});		
			engine.setQueryResult(queryResult);
		}
		
		return combineResults(engines);
	}

}
