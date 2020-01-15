package com.example.main.backend;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;

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
	private LinkedHashSet<URL> images = new LinkedHashSet<URL>();

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
	
	public void addImage(URL url) {
		this.images.add(url);
	}
	
	public LinkedHashSet<URL> getImages(){
		return this.images;
	}
}
