package com.example.main.backend.api;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.SpellChecker;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.example.main.backend.utils.Utils;

import javafx.util.Pair;

@Component
public class SearchAPI {
	
	@Autowired
	DBHandler db;
	
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

	public Query resolveSiteOperator(String a_query) {

		Query q = new Query();
		a_query = a_query.trim();

		// That means, that a side operator is included in that query
		int indexOperator = a_query.indexOf("site:");
		if (indexOperator == -1) {
			q.query = a_query;
		} else if (indexOperator > 0) {
			// Split by site operator --> this is the easy way
			String[] tokens = a_query.split("site:");
			if (tokens.length == 2) {
				q.query = tokens[0];
				String site = tokens[1];
				try {
					URL url = new URL(site.trim());
					q.site = url;
				} catch (Exception ex) {
					System.out.println("Not a valid site. Operator ignored.");
				}
			}
		} else if (indexOperator == 0) {
			String[] tokens = a_query.split(" ");
			// Check if site operator and url are combined in one token
			String site = "";
			if (tokens[0].trim().length() > 5) {
				// Yes they are combined
				site = tokens[0].trim().substring(5);
				q.query = tokens[1];
			} else {
				// No there was a space between it
				site = tokens[1].trim();
				q.query = tokens[2];
			}
			// convert to url
			try {
				URL url = new URL("https",site,"/");
				q.site = url;
			} catch (Exception ex) {
				System.out.println("Not a valid site. Operator ignored.");
			}
		}
		return q;
	}

	public SearchResultResponse searchAPIconjunctive(String a_query, int limit, String[] languages) {

		Query q = resolveSiteOperator(a_query);
		SearchResultResponse res = new SearchResultResponse(q.query, limit);

		try {
			res = db.searchConjunctiveQuery(q.query, limit, languages, res);
			int cw = db.getCollectionSize();
			res.setCollectionSize(cw);
			res = db.getStats(q.query, res);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} 
		res.filterResultsWithSite(q.site);
		return res;
	}

	public SearchResultResponse searchAPIdisjunctive(String a_query, int limit, String[] languages) {

		Query q = resolveSiteOperator(a_query);
		SearchResultResponse res = new SearchResultResponse(q.query, limit);

		try {
			res = db.searchDisjunctiveQuery(q.query, limit, languages, res);
			int cw = db.getCollectionSize();
			res.setCollectionSize(cw);
			res = db.getStats(q.query, res);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		res.filterResultsWithSite(q.site);
		return res;
	}

	public void updateScores() {
		try {
			db.computePageRank(0.1,0.001);
			db.updateScores();

		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public String getDidYouMeanQuery(String query)  {
		
		String altQuery = null;
		Connection con = null;
		try {
			List<String> terms = Utils.getTermsWithoutQuotes(query);
			terms.addAll(Utils.getTermsInQuotes(query));
			String[] termsArr = (String[]) terms.toArray(new String[terms.size()]);
			con = db.getConnection();
			SpellChecker spellChecker = new SpellChecker();
			Map<String, List<Pair<String, Integer>>> relTerms = spellChecker.findRelatedTermsForLessFrequentTerms(termsArr, con);
			
			altQuery = spellChecker.findBestAlternateQuery(terms, relTerms, con);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(con != null)
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		
		return altQuery;
	}
}
