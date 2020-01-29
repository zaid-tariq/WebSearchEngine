package com.example.main.backend.api.responseObjects;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.example.main.backend.dao.Snippet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResultResponse {
	
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
	
	public void addSearchResultItem(int rank, String url, float score, Snippet snippet) {
		this.resultList.add(new SearchResultItem(rank, url, score, snippet));
	}
	
	public void addSearchResultItem(int rank, String url, String url2, float score, Snippet snippet) {
		SearchResultItem i = new SearchResultItem(rank, url, score, snippet);
		i.setUrl2(url2);
		this.resultList.add(i);
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
	
	public void filterResultsWithSite(URL site) {
		
		if(site == null)
			return;
		List<SearchResultItem> filteredList = new ArrayList<SearchResultItem>();
		
		for(SearchResultItem resItem : this.resultList) {
			
			try {
				URL url = new URL(resItem.url.trim());
				if(url.getHost().equals(site.getHost())) {
					filteredList.add(resItem);
				}
				
			} catch (MalformedURLException e) {
				
				System.out.println("Url skipped because of site filter!");
			}
		}
		
		this.resultList = filteredList;
	}
	
	
	public void printResult() {
		
		for(SearchResultItem resItem : this.resultList) {
			System.out.println(resItem.rank +" , "+resItem.url+", "+resItem.score);
		}
		
	}


}
