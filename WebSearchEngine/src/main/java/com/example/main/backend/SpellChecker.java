package com.example.main.backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpellChecker {

	int term_distance_threshold = 2; // what distance to consider while classifying term as similar
	int rare_threshold = 5; // what document_frequency to consider rare

	public Map<String, List<Map.Entry<String, Integer>>> findRelatedTermsForLessFrequentTerms(String[] a_terms,
			Connection con) throws SQLException {

		/*
		 * check if the document_frequency of some term_in_query is zero or very low.
		 * Then find closely related terms in database using levenshtein distance.
		 * Ignore stop words while making this check
		 */

		Map<String, List<Map.Entry<String, Integer>>> relatedTerms = new HashMap<String, List<Map.Entry<String, Integer>>>();
		PreparedStatement sql = con.prepareStatement(
				"SELECT * from get_related_terms_to_non_existant_or_rare_terms(?,?,?) ORDER BY distance FETCH FIRST 10 ROWS ONLY");
		sql.setArray(1, con.createArrayOf("text", a_terms));
		sql.setInt(2, term_distance_threshold);
		sql.setInt(3, rare_threshold);
		sql.execute();
		ResultSet results = sql.getResultSet();
		while (results.next()) {
			String term = results.getString(1);
			String relatedTerm = results.getString(2);
			int dist = results.getInt(3);
			if (!relatedTerms.containsKey(term))
				relatedTerms.put(term, new ArrayList<Map.Entry<String, Integer>>());
			List<Map.Entry<String, Integer>> tempRelTerms = relatedTerms.get(term);
			tempRelTerms.add(new AbstractMap.SimpleEntry<String, Integer>(relatedTerm, dist));
		}

		return relatedTerms;
	}

	public String findBestAlternateQuery(List<String> a_origQueryTerms,
			Map<String, List<Map.Entry<String, Integer>>> a_query_and_related_terms, Connection con) throws SQLException {

		LinkedList<Map.Entry<String, Integer>> queries = new LinkedList<Map.Entry<String, Integer>>();
		queries.add(new AbstractMap.SimpleEntry<String, Integer>("", 0));
		
		LinkedList<Map.Entry<String,Integer>> newQueries = new LinkedList<Map.Entry<String,Integer>>();
		for (String qTerm : a_origQueryTerms) {
			for (Map.Entry<String, Integer> relatedTerm : a_query_and_related_terms.get(qTerm)) {
				for(Map.Entry<String,Integer> query : queries) {
					newQueries.add(new AbstractMap.SimpleEntry<String,Integer>((query.getKey() +" "+ relatedTerm.getKey()).trim(), query.getValue()+relatedTerm.getValue()));
				}
			}
			queries = newQueries;
			newQueries = new LinkedList<Map.Entry<String,Integer>>();
		}
		Map.Entry<String,Integer> best = null;
		for(Map.Entry<String,Integer> q : queries) {
			if(best == null || best.getValue() > q.getValue()) {
				best = q;
			}
		}
		
		return best.getKey();
	}
}
