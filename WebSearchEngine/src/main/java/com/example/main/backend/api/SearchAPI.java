package com.example.main.backend.api;

public class SearchAPI {

	private String searchQuery;
	private int limit;
	
	public SearchAPI(String a_searchQuery, int a_limit) {
		this.searchQuery = a_searchQuery;
		this.limit = a_limit;
	}
}
