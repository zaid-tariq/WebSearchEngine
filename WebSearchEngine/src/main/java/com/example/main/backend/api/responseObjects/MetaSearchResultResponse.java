package com.example.main.backend.api.responseObjects;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaSearchResultResponse {
	
	public List<MetaSearchResultItem> resultList = new ArrayList<MetaSearchResultItem>();
	public List<StatItem> stat = new ArrayList<StatItem>();
	public Query query;
	public int cw;
	
	public MetaSearchResultResponse() {
	}
	
	public MetaSearchResultResponse(String a_searchQuery, int a_limit) {
		this.query = new Query(a_limit, a_searchQuery);
	}
	
	public void addStat(int df, String term) {
		this.stat.add(new StatItem(df, term));
	}
	
	public void addSearchResultItem(int rank, String url, float score, String source) {
		this.resultList.add(new MetaSearchResultItem(rank, url, score, source));
	}
	
	public void setCollectionSize(int size) {
		this.cw = size;
	}

	public List<MetaSearchResultItem> getResultList() {
		return resultList;
	}

	public void setResultList(List<MetaSearchResultItem> resultList) {
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
	
	public void printResult() {
		
		for(MetaSearchResultItem resItem : this.resultList) {
			System.out.println(resItem.rank +" , "+resItem.url+", "+resItem.score);
		}
		
	}


}
