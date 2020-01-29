package com.example.main.backend.api.responseObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Query{
	public int k;
	public String query;
	public Query() {}
	public Query(int k, String query) {
		this.k = k;
		this.query = query;
	}
}
