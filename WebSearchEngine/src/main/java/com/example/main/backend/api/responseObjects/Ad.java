package com.example.main.backend.api.responseObjects;

public class Ad {

	private int id;
	private String text;
	private String imageURL;
	private double score;
	private String url;
	private String[] ngrams;

	public Ad(int id, String text, String imageURL, double score, String url, String[] ngrams) {
		this.id = id;
		this.text = text;
		this.imageURL = imageURL;
		this.score = score;
		this.url = url;
		this.ngrams = ngrams;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String[] getNgrams() {
		return ngrams;
	}

	public void setNgrams(String[] ngrams) {
		this.ngrams = ngrams;
	}
	
}