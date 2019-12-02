package com.example.main.backend;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.main.backend.api.responseObjects.SearchResultResponse;

public class DBHandler {

	public void computeTfIdf(Connection a_conn) throws SQLException {
		PreparedStatement query = a_conn.prepareStatement("CALL update_tf_idf_scores()");
		query.execute();
	}

	public SearchResultResponse searchConjunctiveQuery(Connection a_conn, String query, int a_k, SearchResultResponse a_response) throws SQLException {
		
		List<String> searchTerms = getTermsInQuotes(query);
		String[] searchTermsArr = getTermsInQuotes(query).toArray(new String[searchTerms.size()]);
		PreparedStatement sql = a_conn.prepareStatement("SELECT * from conjunctive_search(?, ?)");
		sql.setArray(1,  a_conn.createArrayOf("text", searchTermsArr));
		sql.setInt(2, a_k);
		sql.execute();
		ResultSet results = sql.getResultSet();
		if(a_response == null)
			a_response = new SearchResultResponse();
		int rank = 1;
		while (results.next()) {
			String url = results.getString(1);
			float score = results.getFloat(2);
			a_response.addSearchResultItem(rank++, url, score);
		}
		return a_response;
	}

	public SearchResultResponse searchDisjunctiveQuery(Connection a_conn, String query, int a_k, SearchResultResponse a_response) throws SQLException {
		
		List<String> searchTerms = new ArrayList<String>();
		for(String subQuery : getTermsInQuotes("\"" + query + "\"")) {
			for(String term : subQuery.split(" ")) {
				term = term.trim();
				if(term.length() > 0) {
					searchTerms.add(term);
					System.out.println(term);
				}
			}
		}
		
		String[] searchTermsArr = (String[]) searchTerms.toArray(new String[searchTerms.size()]);
		List<String> requiredTerms = getTermsInQuotes(query);
		String[] requiredTermsArr = (String[]) requiredTerms.toArray(new String[requiredTerms.size()]);
		
		PreparedStatement sql = a_conn.prepareStatement("SELECT * from disjunctive_search(?,?,?)");
		sql.setArray(1, a_conn.createArrayOf("text", searchTermsArr));
		sql.setArray(2, a_conn.createArrayOf("text", requiredTermsArr));
		sql.setInt(3, a_k);
		sql.execute();
		ResultSet results = sql.getResultSet();
		if(a_response == null)
			a_response = new SearchResultResponse();
		int rank = 1;
		while (results.next()) {
			String url = results.getString(1);
			float score = results.getFloat(2);
			a_response.addSearchResultItem(rank++, url, score);
		}
		return a_response;
	}
	
	
	public SearchResultResponse getStats(Connection a_conn, String query, SearchResultResponse a_response) throws SQLException {
		
		List<String> terms = new ArrayList<String>();
		for(String subQuery : getTermsInQuotes("\"" + query + "\"")) {
			for(String term : subQuery.split(" ")) {
				term = term.trim();
				if(term.length() > 0) {
					terms.add(term);
					System.out.println(term);
				}
			}
		}
		
		terms.addAll(getTermsInQuotes(query));
		
		String[] termsArr = (String[]) terms.toArray(new String[terms.size()]);
		
		PreparedStatement sql = a_conn.prepareStatement("SELECT * from get_term_frequencies(?)");
		sql.setArray(1, a_conn.createArrayOf("text", termsArr));
		sql.execute();
		ResultSet results = sql.getResultSet();
		
		if(a_response == null)
			a_response = new SearchResultResponse();
		
		while (results.next()) {
			String term = results.getString(1);
			int df = results.getInt(2);
			a_response.addStat(df, term);
		}
		
		return a_response;
	}
	
	
	public int getCollectionSize(Connection a_conn) throws SQLException {
		PreparedStatement sql = a_conn.prepareStatement("SELECT COUNT(docid) from documents");
		sql.execute();
		ResultSet results = sql.getResultSet();
		results.next();
		return results.getInt(1);
	}

	
	private List<String> getTermsInQuotes(String query) {
		
		List<String> terms = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\"([^\"]*)\"");
		Matcher matcher = pattern.matcher(query);
		while(matcher.find()){
			String term = matcher.group(1);
			terms.add(term);
			System.out.println(term);
		}
		
		return terms;	
	}
	
}