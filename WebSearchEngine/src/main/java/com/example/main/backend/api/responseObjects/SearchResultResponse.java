package com.example.main.backend.api.responseObjects;

import java.util.ArrayList;
import java.util.List;

public class SearchResultResponse {
	
	class SearchResultItem{
		public int rank;
		public String url;
		public float score;
		
		public SearchResultItem(int r, String u, float s) {
			this.rank = r;
			this.url = u;
			this.score = s;
		}
	}
	
	class Query{
		public int k;
		public String query;
		public Query(int k, String query) {
			this.k = k;
			this.query = query;
		}
	}
	
	class StatItem{
		
		public int df;
		public String term;
		public StatItem(int k, String term) {
			this.df = k;
			this.term = term;
		}
	}
	
	public List<SearchResultItem> resultList = new ArrayList<SearchResultItem>();
	public List<StatItem> stat = new ArrayList<StatItem>();
	public Query query;
	public int cw;
	
	public SearchResultResponse(String a_searchQuery, int a_limit) {
		this.query = new Query(a_limit, a_searchQuery);
	}
	
	public void addStat(int df, String term) {
		this.stat.add(new StatItem(df, term));
	}
	
	public void addSearchResultItem(int rank, String url, float score) {
		this.resultList.add(new SearchResultItem(rank, url, score));
	}
	
	public void setCollectionSize(int size) {
		this.cw = size;
	}


}
