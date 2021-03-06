package com.example.main.backend.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.Soundbank;

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
	
	public List<Ad> getAds(String query, int limit) {
		List<Ad> ads;
		try {
			ads = db.getAllAds();
			Set<String> terms = new HashSet<String>();
			for(String s : QueryParser.getTermsInQuotes(query)){
				terms.add(s.toLowerCase());
			}
			
			for(String s : QueryParser.getTermsWithoutQuotes_2(query)) {
				terms.add(s.toLowerCase());
			}
			
			//Calculate scores
			for(Ad ad : ads) {
				double score = 0.0;
				for(String ngram : ad.getNgrams()) {
					String[] w = ngram.split("\\s+");
					int containedWords = 0;
					for(String word : w) {
						if(terms.contains(word.toLowerCase())){
							containedWords++;
						}
					}
					score += w.length * containedWords;
				}
				ad.setScore(score);
			}
			
			ads.sort(Comparator.comparingDouble(Ad::getScore).reversed());
			return ads.subList(0, Math.min(4,ads.size()));
		} catch (SQLException e) {
			e.printStackTrace();
			return new ArrayList<Ad>();
		}
	}
}
