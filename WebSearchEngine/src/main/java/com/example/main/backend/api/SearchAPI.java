package com.example.main.backend.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.responseObjects.Ad;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.example.main.backend.dao.Query;
import com.example.main.backend.utils.QueryParser;
import com.example.main.backend.utils.SpellChecker;

@Component
public class SearchAPI {

	@Autowired
	DBHandler db;
	
	public static final int DOCUMENT_MODE = 1;
	public static final int IMAGE_MODE = 2;

	public SearchResultResponse searchAPIconjunctive(String a_query, int limit, String[] languages) {

		Query q = QueryParser.resolveSiteOperator(a_query);
		SearchResultResponse res = new SearchResultResponse(q.query, limit);

		try {
			res = db.searchConjunctiveQuery(q.query, limit, languages, res, SearchAPI.DOCUMENT_MODE);
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

	public SearchResultResponse searchAPIdisjunctive(String a_query, int limit, String[] languages, int scoringMethod, int searchMode) {

		Query q = QueryParser.resolveSiteOperator(a_query);
		SearchResultResponse res = new SearchResultResponse(q.query, limit);

		try {
			res = db.searchDisjunctiveQuery(q.query, limit, languages, res, scoringMethod, searchMode);
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
	
	public String getDidYouMeanQuery(String query)  {
		String altQuery = null;
		Connection con = null;
		try {
			List<String> terms = QueryParser.getTermsWithoutQuotes_2(query);
			terms.addAll(QueryParser.getTermsInQuotes(query));
			String[] termsArr = (String[]) terms.toArray(new String[terms.size()]);
			con = db.getConnection();
			SpellChecker spellChecker = new SpellChecker();
			Map<String, List<Map.Entry<String, Integer>>> relTerms = spellChecker.findRelatedTermsForLessFrequentTerms(termsArr, con);
			
			if(relTerms.size() > 0 )
				altQuery = spellChecker.findBestAlternateQuery(terms, relTerms, con);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (con != null)
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}

		return altQuery;
	}
	
	public List<Ad> getAds() {
		//TODO: get best ads
		return new ArrayList<Ad>();
	}
}
