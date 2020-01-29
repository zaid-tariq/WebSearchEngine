package com.example.main.backend.api.responseObjects;

import java.util.ArrayList;
import java.util.List;

public class MetaSearchResponseConfigObject {

	public class Pair{
		String url;
		boolean enabled;
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public boolean isEnabled() {
			return enabled;
		}
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		Pair(String a_url, boolean a_enabled){
			this.url = a_url;
			this.enabled = a_enabled;
		}
		
	}
	public boolean requestStatus;
	List<Pair> urls;
	
	public List<Pair> getUrls() {
		return urls;
	}
	public void setUrls(List<Pair> urls) {
		this.urls = urls;
	}
	public boolean isRequestStatus() {
		return requestStatus;
	}
	public void setRequestStatus(boolean requestStatus) {
		this.requestStatus = requestStatus;
	}
	public void addEngineUrl(String url, boolean enabled) {
		if(urls == null)
			urls = new ArrayList<Pair>();
		urls.add(new Pair(url, enabled));
	}
}
