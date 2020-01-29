package com.example.main.backend.api.responseObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatItem{
	
	public int df;
	public String term;
	public StatItem() {}
	public StatItem(int k, String term) {
		this.df = k;
		this.term = term;
	}
	public int getDf() {
		return df;
	}
	public void setDf(int df) {
		this.df = df;
	}
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
}
