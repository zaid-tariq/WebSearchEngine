package com.example.main.backend.api.responseObjects;

import java.util.ArrayList;
import java.util.List;

public class SearchResultResponse {
	
	public class SearchResultItem{
		public int rank;
		public String url;
		public float score;
		
		public SearchResultItem(int r, String u, float s) {
			this.rank = r;
			this.url = u;
			this.score = s;
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
	}
	
	public class Query{
		public int k;
		public String query;
		public Query(int k, String query) {
			this.k = k;
			this.query = query;
		}
	}
	
	public class StatItem{
		
		public int df;
		public String term;
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
	
	public List<SearchResultItem> resultList = new ArrayList<SearchResultItem>();
	public List<StatItem> stat = new ArrayList<StatItem>();
	public Query query;
	public int cw;
	
	public SearchResultResponse() {
	}
	
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

	public List<SearchResultItem> getResultList() {
		return resultList;
	}

	public void setResultList(List<SearchResultItem> resultList) {
		this.resultList = resultList;
	}

	public List<StatItem> getStat() {
		return stat;
	}

	public void setStat(List<StatItem> stat) {
		this.stat = stat;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public int getCw() {
		return cw;
	}

	public void setCw(int cw) {
		this.cw = cw;
	}


}
