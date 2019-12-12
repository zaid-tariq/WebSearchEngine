package com.example.main.backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;

@SuppressWarnings("restriction")
public class SpellChecker {
	
	int term_distance_threshold = 2; //what distance to consider while classifying term as similar
	int rare_threshold = 5; //what document_frequency to consider rare 	
	
	
	public Map<String, List<Pair<String, Integer>>> findRelatedTermsForLessFrequentTerms(String[] a_terms, Connection con) throws SQLException {
		
		/* 
		 * check if the document_frequency of some term_in_query is zero or very low. 
		 * Then find closely related terms in database using levenshtein distance.
		 * Ignore stop words while making this check
		 */
		
		Map<String, List<Pair<String, Integer>>> relatedTerms = new HashMap<String, List<Pair<String, Integer>>>();
		PreparedStatement sql = con.prepareStatement("SELECT * from get_related_terms_to_non_existant_or_rare_terms(?,?,?)");
		sql.setArray(1, con.createArrayOf("text", a_terms));
		sql.setInt(2, term_distance_threshold);
		sql.setInt(3, rare_threshold);
		sql.execute();
		ResultSet results = sql.getResultSet();
		while(results.next()) {
			
			String term = results.getString(1);
			String relatedTerm = results.getString(2);
			int dist = results.getInt(3);
			if(!relatedTerms.containsKey(term))
				relatedTerms.put(term, new ArrayList<Pair<String, Integer>>());
			List<Pair<String, Integer>> tempRelTerms = relatedTerms.get(term);
			tempRelTerms.add(new Pair<String, Integer>(relatedTerm, dist));
		}		
		
		return relatedTerms;
	}
	
	
	public String findBestAlternateQuery(List<String> a_origQueryTerms, Map<String, List<Pair<String, Integer>>> a_query_and_related_terms, Connection con) throws SQLException {
		
		/*
		 * Idea: 
		 * 1- Make several versions of query using the related-terms 
		 * 2- Divide each such alternate query in pairs of terms. For each pair, count number of document the pair appears in. Return the query whose pairs yield the highest count.
		 */
		
		List<List<String>> related_query_terms = new ArrayList<List<String>>();
		
		for(String queryTerm : a_origQueryTerms) {
			List<String> curr_rel_terms = new ArrayList<String>();
			List<Pair<String, Integer>> relTerms = a_query_and_related_terms.get(queryTerm);
			for(Pair<String, Integer> relTermPair: relTerms) {
				String relTerm = relTermPair.getKey();
				curr_rel_terms.add(relTerm);
			}
			related_query_terms.add(curr_rel_terms);
		}
		
		List<List<String>> queryCombinations = findAllPermutations(a_origQueryTerms, related_query_terms);
		
		Pair<List<String>, Integer> bestQuery = new Pair<List<String>, Integer>(null, 0);
		PreparedStatement sql = con.prepareStatement("SELECT * from get_related_terms_to_non_existant_or_rare_terms(?,?,?)");
		for(List<String> alternateQuery: queryCombinations) {
			
			//TODO: remove stop words at this stage, before dividing the query into pairs
			List<String> pairs = getAllPairsOfTermsInQuery(alternateQuery);
			sql.clearParameters();
			sql.setArray(1, con.createArrayOf("text", (String[]) pairs.toArray(new String[pairs.size()])));
			sql.execute();
			ResultSet rs = sql.getResultSet();
			rs.next();
			int score = rs.getInt(1);
			rs.close();
			if(score > bestQuery.getValue())
				bestQuery = new Pair<List<String>, Integer>(alternateQuery, score);
		}
		sql.close();
		return bestQuery.getKey().toString();
	}
	
	private List<List<String>> findAllPermutations(List<String> a_origQueryTerms, List<List<String>> a_related_query_terms) {
		
		List<List<String>> permutations = new ArrayList<List<String>>();
		List<List<String>> processQueue = new ArrayList<List<String>>();
		processQueue.add(a_origQueryTerms);
		permutations.add(a_origQueryTerms);
		
		for(int level = 0; level < a_origQueryTerms.size(); level++) {
			processQueue = findPermutationsForGivenLevel(permutations, processQueue, level, a_origQueryTerms.size(), a_related_query_terms);
		}
		return permutations;
	}
	
	private List<List<String>> findPermutationsForGivenLevel(List<List<String>> permutations, List<List<String>> processQueue,
			int level, int size_of_query, List<List<String>> a_related_query_terms) {

		int start_index = level;
		int end_index = size_of_query;
		List<List<String>> newProcessQueue = new ArrayList<List<String>>();
		for(List<String> query : processQueue){
			for(int index_to_change = start_index; index_to_change < end_index; index_to_change++) {
				List<List<String>> alternative_queries = 
					getAllPermutationsAtaSpecificIndexForAGivenBaseQuery(
						query, index_to_change, a_related_query_terms.get(index_to_change));
				permutations.addAll(alternative_queries);
				newProcessQueue.addAll(alternative_queries);
			}
		}
		
		return newProcessQueue;
	}
		
	private List<List<String>> getAllPermutationsAtaSpecificIndexForAGivenBaseQuery(List<String> baseQuery, int index_to_permute, 
			List<String> all_possible_values_at_index){
				
		List<List<String>> res = new ArrayList<List<String>>();
		for(String val : all_possible_values_at_index) {
			List<String> new_alt_query = new ArrayList<String>(baseQuery);
			new_alt_query.set(index_to_permute, val);
			res.add(new_alt_query);
		}
		
		return res;
	}
	
	
	private List<String> getAllPairsOfTermsInQuery(List<String> terms) {
		
		List<String> combs = new ArrayList<String>();
		for(int i = 0; i < terms.size(); i++) {
			String pivot_term = terms.get(i);
			for(int j = i+1; j < terms.size(); j++) {
				String second_term = terms.get(j);
				combs.add(pivot_term+":"+second_term);
			}
			
		}
		return combs;
	}

}
