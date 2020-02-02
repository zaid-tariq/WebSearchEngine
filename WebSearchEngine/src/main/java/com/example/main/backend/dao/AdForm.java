package com.example.main.backend.dao;

public class AdForm {
	private String url;
	private String imageURL;
	private float pricePerClick;
	private float totalBudget;
	private String ngrams;
	private String description;
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getImageURL() {
		return imageURL;
	}
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	public float getPricePerClick() {
		return pricePerClick;
	}
	public void setPricePerClick(float pricePerClick) {
		this.pricePerClick = pricePerClick;
	}
	public float getTotalBudget() {
		return totalBudget;
	}
	public void setTotalBudget(float totalBudget) {
		this.totalBudget = totalBudget;
	}
	public String getNgrams() {
		return ngrams;
	}
	public void setNgrams(String ngrams) {
		this.ngrams = ngrams;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}