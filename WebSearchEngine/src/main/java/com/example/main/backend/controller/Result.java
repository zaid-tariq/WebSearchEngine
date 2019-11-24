package com.example.main.backend.controller;

public class Result {
	public int rank;
	public String url;
	public float score;

	public Result(int rank, String url, float score) {
		this.rank = rank;
		this.url = url;
		this.score = score;
	}
}
