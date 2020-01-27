package com.example.main.backend.dao;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

public class HTMLDocument {

	public static class Language {
		public static final String GERMAN = "german";
		public static final String ENGLISH = "english";
	}

	private URL url;
	private LinkedHashSet<URL> links = new LinkedHashSet<>();
	private HashMap<String, Integer> termFrequencies = new HashMap<>();
	private String language = null;
	private String content = null;
	private HashMap<Integer, URL> images = new HashMap<Integer, URL>();
	private MultiKeyMap<String, Integer> termDistances = new MultiKeyMap<String, Integer>();

	public HTMLDocument(URL url) {
		this.url = url;
	}

	public void addLink(URL url) {
		links.add(url);
	}

	public void incrementTermFrequency(String term) {
		termFrequencies.computeIfPresent(term, (k, v) -> {
			v = v + 1;
			return v;
		});
		termFrequencies.putIfAbsent(term, 1);
	}

	public URL getUrl() {
		return url;
	}

	public LinkedHashSet<URL> getLinks() {
		return links;
	}

	public HashMap<String, Integer> getTermFrequencies() {
		return termFrequencies;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return this.content;
	}

	public void addImage(URL url, int position) {
		this.images.put(position,url);
	}

	public HashMap<Integer,URL> getImages() {
		return this.images;
	}

	public void addTermDistance(String url, String term, int distance) {
		termDistances.putIfAbsent(new MultiKey<String>(url, term), distance);
	}

	public MultiKeyMap<String, Integer> getTermDistances() {
		return this.termDistances;
	}
}
