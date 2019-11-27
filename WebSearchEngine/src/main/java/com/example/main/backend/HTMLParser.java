package com.example.main.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

import com.shekhargulati.urlcleaner.UrlExtractor;


public class HTMLParser {

	public List<String> stopwords;

	public HTMLParser() throws IOException {
		
		File file = new ClassPathResource("stopwords.txt").getFile();
		stopwords = getStopwordsFromFile(file);
	}

	/**
	 * Parses a given HTML file defined by the URI
	 * 
	 * @param urlToIndex URL to the HTML file
	 * @return HTMLDocument that represents the important stuff of that file
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public HTMLDocument parse(URL urlToIndex) throws IOException {
		HTMLDocument doc = new HTMLDocument(urlToIndex);

		BufferedReader br = new BufferedReader(new InputStreamReader(urlToIndex.openStream()));

		String content = "";
		String line;
		while ((line = br.readLine()) != null) {
			content += line;
		}

		// 1. parse file for meta data e.g. language
		Matcher mLanguage = Pattern.compile("<html[^>]*lang=\"(.*?)\"[^>]*>").matcher(content);
		if (mLanguage.find()) {
			doc.setLanguage(mLanguage.group(1));
		}

		// 2. parse the file for links

		Matcher mLinks = Pattern.compile("href=\"(.*?)\"").matcher(content);
		while (mLinks.find()) {
			try {
				List<String> urls = UrlExtractor.extractUrls(mLinks.group());
				for (String url : urls) {
					doc.addLink(new URL(url));
				}
			} catch (MalformedURLException e) {
				// Yeah it is no URL...
				e.printStackTrace();
			}

		}

		// 3. remove all tags

		// That was not good, because we had a lot of unwanted content

		// Matcher mTags = Pattern.compile("<[^>]*>").matcher(content);
		// content = mTags.replaceAll(" "); // by empty string to split the content

		Pattern rm = Pattern.compile("<[^>]*>");

		LinkedList<String> extractedContent = new LinkedList<String>();

		String specialChars = "(\\?|\\!|\\>|\\/|&nbsp;|&bull;|&amp;|\\.|\\,|\\:|\\;)";

		Matcher hTags = Pattern.compile("<h\\d[^>]*>(.+?)</h\\d>").matcher(content);
		while (hTags.find()) {
			for (String word : hTags.group(1).replaceAll("<[^>]*>", " ").replaceAll(specialChars, "").split("\\s+")) {
				if (!word.trim().equals("")) {
					extractedContent.add(word);
				}
			}
		}

		Matcher aTags = Pattern.compile("<a[^>]*>(.+?)</a>").matcher(content);
		while (aTags.find()) {
			for (String word : aTags.group(1).replaceAll("<[^>]*>", " ").replaceAll(specialChars, "").split("\\s+")) {
				if (!word.trim().equals("")) {
					extractedContent.add(word);
				}
			}
		}

		Matcher pTags = Pattern.compile("<p[^>]*>(.+?)</p>").matcher(content);
		while (pTags.find()) {
			for (String word : pTags.group(1).replaceAll("<[^>]*>", " ").replaceAll(specialChars, "").split("\\s+")) {
				if (!word.trim().equals("")) {
					extractedContent.add(word);
				}
			}
		}
		
		Matcher spanTags = Pattern.compile("<span[^>]*>(.+?)</span>").matcher(content);
		while (spanTags.find()) {
			for (String word : spanTags.group(1).replaceAll("<[^>]*>", " ").replaceAll(specialChars, "").split("\\s+")) {
				if (!word.trim().equals("")) {
					extractedContent.add(word);
				}
			}
		}

		// 4. remove all stopwords, stemming and term frequency calculation at the same
		// time for efficiency
		String processedContent = "";
		Stemmer stemmer = new Stemmer();
		Queue<String> words = extractedContent; // old: Arrays.asList(content.split(" "))
		while (!words.isEmpty()) {
			String word = words.remove().toLowerCase();
			if (!stopwords.contains(word)) {
				// Keep on process else drop
				stemmer.add(word.toCharArray(), word.length());
				stemmer.stem();
				String stemmedWord = stemmer.toString();
				doc.incrementTermFrequency(stemmedWord);
				processedContent += stemmedWord;
			}
		}

		doc.setContent(processedContent);
		br.close();
		return doc;
	}

	/**
	 * Turns the file which contains all stopwords into an list of stopwords
	 * 
	 * @param file File that contains all stopwords
	 * @return List of stopwords
	 * @throws IOException
	 */
	private List<String> getStopwordsFromFile(File file) throws IOException {
		List<String> s = new ArrayList<>();
		BufferedReader r = new BufferedReader(new FileReader(file));

		String word;
		while ((word = r.readLine()) != null) {
			s.add(word.toLowerCase().trim());
		}
		r.close();
		return s;
	}
}
