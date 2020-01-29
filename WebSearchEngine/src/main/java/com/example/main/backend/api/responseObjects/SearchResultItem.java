package com.example.main.backend.api.responseObjects;

import com.example.main.backend.dao.Snippet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResultItem{
	public int rank;
	public String url;
	public String url2;
	public float score;
	public Snippet snippet;
	
	public SearchResultItem() {}
	
	public SearchResultItem(int r, String u, float s, Snippet snippet) {
		this.rank = r;
		this.url = u;
		this.score = s;
		this.snippet = snippet;
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
	
	public String getUrl2() {
		return url2;
	}

	public void setUrl2(String url2) {
		this.url2 = url2;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}
	
	public Snippet getSnippet() {
		return snippet;
	}
	
	public void setSnippet(Snippet snippet) {
		this.snippet = snippet;
	}
}
