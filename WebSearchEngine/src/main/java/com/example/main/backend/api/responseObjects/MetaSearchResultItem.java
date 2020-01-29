package com.example.main.backend.api.responseObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaSearchResultItem{
	public int rank;
	public String url;
	public float score;
	public String source;
	
	public MetaSearchResultItem() {}
	
	
	public MetaSearchResultItem(int r, String u, float s, String s2) {
		this.rank = r;
		this.url = u;
		this.score = s;
		this.source = s2;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
}
