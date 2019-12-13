package com.example.main.backend.dao;

import java.util.HashMap;
import java.lang.Math;

public class DBResponseDocument {
	
	public class score_pair{
		public Float okapi_score;
		public Float tfidf_score; 
	}
	
	public String url;
	public HashMap<String, score_pair> terms_scores;
	public double cosSim;
	public DBResponseDocument(String url2) {
		this.url = url2;
	}
	

	public void add_term(String term, float tfidf_score, float okapi_score) {
		score_pair pair = new score_pair();
		pair.okapi_score = okapi_score;
		pair.tfidf_score = tfidf_score;
		if(this.terms_scores == null)
			terms_scores = new HashMap<String, score_pair>();
			
		this.terms_scores.put(term, pair);
	}
	
	public double getCosineSimilarity_tfIdf(DBResponseDocument doc2) {
		
		double dotProd = 0, norm1 = 0, norm2 = 0;
		
		for(String key : this.terms_scores.keySet()) {
			if(doc2.terms_scores.containsKey(key)) {
				dotProd += (this.terms_scores.get(key).tfidf_score * doc2.terms_scores.get(key).tfidf_score);
			}
			
			//calculate norm alongside
			
			norm1 += Math.pow(this.terms_scores.get(key).tfidf_score, 2);
		}
		
		for(String key : doc2.terms_scores.keySet()) 
			norm2 += Math.pow(doc2.terms_scores.get(key).tfidf_score, 2);
		
		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		
		Double normProd = norm1*norm2;
		if(normProd == 0)
			return 0;
		else
			return dotProd/normProd;
	}
	
public double getCosineSimilarity_okapi(DBResponseDocument doc2) {
		
		double dotProd = 0, norm1 = 0, norm2 = 0;
		
		for(String key : this.terms_scores.keySet()) {
			if(doc2.terms_scores.containsKey(key)) {
				dotProd += (this.terms_scores.get(key).okapi_score * doc2.terms_scores.get(key).okapi_score);
			}
			
			//calculate norm alongside
			
			norm1 += Math.pow(this.terms_scores.get(key).okapi_score, 2);
		}
		
		for(String key : doc2.terms_scores.keySet()) 
			norm2 += Math.pow(doc2.terms_scores.get(key).okapi_score, 2);
		
		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		
		Double normProd = norm1*norm2;
		if(normProd == 0)
			return 0;
		else
			return dotProd/normProd;
	}

}
