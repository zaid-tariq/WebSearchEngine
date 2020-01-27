package com.example.main.backend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import com.example.main.backend.dao.HTMLDocument;

public class Indexer {

	public static HTMLDocument index(URL urlToIndex) throws IOException, URISyntaxException {
		HTMLParser htmlParser = new HTMLParser();
		HTMLDocument doc = htmlParser.parse(urlToIndex);
		return doc;
	}
}
