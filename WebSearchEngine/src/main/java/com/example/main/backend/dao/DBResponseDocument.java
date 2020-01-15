package com.example.main.backend.dao;

import java.util.HashMap;
import java.util.List;
import java.lang.Math;

public class DBResponseDocument {

	public class Scores {
		public Float okapi_score;
		public Float tfidf_score;
		public Float combined;
	}

	public String url;
	public HashMap<String, Scores> terms_scores;
	private double cosSim;
	public double pageRank;
	public int scoringMethod;
	double alpha = 0.7;
	private String snippet;
	private List<String> notOccurringTerms;

	public DBResponseDocument(String url2) {
		this.url = url2;
	}

	public void add_term(String term, float tfidf_score, float okapi_score) {
		Scores scores = new Scores();
		scores.okapi_score = okapi_score;
		scores.tfidf_score = tfidf_score;
		if (this.terms_scores == null)
			terms_scores = new HashMap<String, Scores>();

		this.terms_scores.put(term, scores);
	}

	private double calculateCombinedScore() {

		return (alpha * cosSim) + ((1 - alpha) * pageRank);
	}

	public void calculateCosineSimilarity(DBResponseDocument doc2) {

		double dotProd = 0, norm1 = 0, norm2 = 0;

		for (String key : this.terms_scores.keySet()) {
			if (doc2.terms_scores.containsKey(key)) {
				dotProd += (this.getScore(terms_scores.get(key)) * doc2.getScore(terms_scores.get(key)));
			}

			// calculate norm alongside

			norm1 += Math.pow(this.getScore(terms_scores.get(key)), 2);
		}

		for (String key : doc2.terms_scores.keySet())
			norm2 += Math.pow(doc2.getScore(doc2.terms_scores.get(key)), 2);

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);

		Double normProd = norm1 * norm2;
		if (normProd == 0)
			this.cosSim = 0;
		else
			this.cosSim = dotProd / normProd;

		if (this.scoringMethod == 3) {
			this.cosSim = this.calculateCombinedScore();
		}
	}

	private float getScore(Scores score) {
		if (this.scoringMethod == 1) {
			return score.tfidf_score;
		} else {
			return score.okapi_score;
		}
	}

	public double getCosSimScore() {
		return this.cosSim;
	}
	
	public String getSnippet() {
		return this.snippet;
	}
	
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public List<String> getNotOccurringTerms(){
		return this.notOccurringTerms;
	}
	
	public void setNotOccurringTerms(List<String> notOccuringTerms) {
		this.notOccurringTerms = notOccuringTerms;
	}

}
