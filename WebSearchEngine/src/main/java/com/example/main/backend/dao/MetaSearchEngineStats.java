package com.example.main.backend.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.example.main.backend.api.responseObjects.MetaSearchResultResponse;

public class MetaSearchEngineStats {

	String url;
	int cw;
	List<TermStats> terms = new ArrayList<TermStats>();
	List<TermStats> unknown_terms = new ArrayList<TermStats>();
	Future<MetaSearchResultResponse> queryResult;

	public void addTerm(String term, Float t_score, Float i_score) {
		TermStats termS = new TermStats();
		termS.setTerm(term);
		termS.setI_score(i_score);
		termS.setT_score(t_score);
		terms.add(termS);
	}
	
	public void upateTermStat(String a_term, int df) {
		for(TermStats term: terms)
			if(term.getTerm() != null && term.getTerm().equals(a_term)) {
				term.setDf(df);
				return;
			}
		unknown_terms.add(new TermStats(a_term, df));
	}

	public Double compute_r_dash_score() {
		double b = 0.4;
		double r_min = 0.0;
		double r_max = 0.0;
		double r_i = 0.0;
		for(TermStats termStat:terms) {
			if(termStat.term == null)
				continue;
			r_i += (b + (1-b) * termStat.getI_score() * termStat.getT_score());
			r_min += (b + (1-b) * termStat.getI_score()); //set T = 1
			r_max += b; //set T = 0
		}
		if(r_max-r_min > 0)
			return (r_i - r_min) / (r_max-r_min);
		return 0.0;
	}	
	
	
	public Future<MetaSearchResultResponse> getQueryResult() {
		return queryResult;
	}

	public void setQueryResult(Future<MetaSearchResultResponse> queryResult) {
		this.queryResult = queryResult;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public int getCw() {
		return cw;
	}
	public void setCw(int cw) {
		this.cw = cw;
	}
	public List<TermStats> getTerms() {
		return terms;
	}
	public List<TermStats> getUnknownTerms() {
		return unknown_terms;
	}
	public void setTerms(List<TermStats> terms) {
		this.terms = terms;
	}
	
	public class TermStats{
		String term;
		Float t_score;
		Float i_score;
		int df;
		TermStats(){}
		TermStats(String term, int df){
			this.term = term;
			this.df = df;
		}
		public String getTerm() {
			return term;
		}
		public void setTerm(String term) {
			this.term = term;
		}
		public Float getT_score() {
			return t_score;
		}
		public void setT_score(Float t_score) {
			this.t_score = t_score;
		}
		public Float getI_score() {
			return i_score;
		}
		public void setI_score(Float i_score) {
			this.i_score = i_score;
		}
		public int getDf() {
			return df;
		}
		public void setDf(int df) {
			this.df = df;
		}
	}
}
