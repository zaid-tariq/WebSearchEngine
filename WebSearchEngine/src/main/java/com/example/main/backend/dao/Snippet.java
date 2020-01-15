package com.example.main.backend.dao;

import java.util.List;

public class Snippet {
	
	List<String> notOccurringTerms;
	String text;
	double score;
	
	public Snippet(String snippet, double score) {
		this.text = snippet;
		this.score = score;
	}
	
	public Snippet(String snippet, double score, List<String> notOccurringTerms) {
		this.text = snippet;
		this.score = score;
		this.notOccurringTerms = notOccurringTerms;
	}

	public List<String> getNotOccurringTerms() {
		return notOccurringTerms;
	}

	public void setNotOccurringTerms(List<String> notOccuringTerms) {
		this.notOccurringTerms = notOccuringTerms;
	}

	public String getText() {
		return text;
	}

	public void setText(String snippet) {
		this.text = snippet;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
}