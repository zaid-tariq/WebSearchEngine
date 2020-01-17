package com.example.main.backend.dao;

import java.net.URL;

public class Query {
	public String query;
	public URL site;

	public Query() {
	}

	public Query(String q, URL u) {
		this.query = q;
		this.site = u;
	}
}
