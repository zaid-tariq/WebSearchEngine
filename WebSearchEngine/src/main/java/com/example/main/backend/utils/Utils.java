package com.example.main.backend.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;

import com.example.main.backend.Stemmer;
import com.example.main.backend.dao.Snippet;

public class Utils {

	public static File createTempFileFromInputStream(String a_resourceName) throws IOException {

		File tempFile = File.createTempFile(a_resourceName, null);
		tempFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempFile);
		IOUtils.copy(new ClassPathResource(a_resourceName).getInputStream(), out);
		return tempFile;
	}

	public static List<String> getTermsInQuotes(String query) {

		List<String> terms = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\"([^\"]*)\"");
		Matcher matcher = pattern.matcher(query);
		while (matcher.find()) {
			String term = matcher.group(1);
			terms.add(term);
		}

		return terms;
	}

	public static List<String> getTermsWithoutQuotes(String query) {
		List<String> searchTerms = new ArrayList<String>();
		for (String subQuery : Utils.getTermsInQuotes("\"" + query + "\"")) {
			for (String term : subQuery.split(" ")) {
				term = term.trim();
				if (term.length() > 0) {
					searchTerms.add(term);
				}
			}
		}

		return searchTerms;
	}

	/**
	 * Returns for each term the number of occurrences in the content
	 * 
	 * @param terms
	 * @param content
	 * @return
	 */
	public static TreeMap<String, Integer> getOrderedTermsByFrequency(List<String> terms, String content) {
		TreeMap<String, Integer> map = new TreeMap<String, Integer>();

		for (String term : terms) {
			term = term.toLowerCase();
			String input = content.toLowerCase();
			int index = input.indexOf(term);
			int occurrences = 0;
			while (index != -1) {
				occurrences++;
				input = input.substring(index + 1);
				index = input.indexOf(term);
			}
			map.put(term, occurrences);
		}
		return map;
	}

	public static String minTerm(List<String> terms, TreeMap<String, Integer> freq) {
		String minString = null;
		int currentMin = Integer.MAX_VALUE;
		for (String t : terms) {
			if(freq.containsKey(t) && freq.get(t) < currentMin) {
				minString = t;
				currentMin = freq.get(t);
			}
		}
		return minString;
	}
	
	public static Snippet getBestSnippet(List<Snippet> snippets) {
		Snippet best = null;
		for(Snippet s: snippets) {
			if(best == null || best.getScore() < s.getScore()) {
				best = s;
			}
		}
		return best;
	}

	/**
	 * Wraps an string with the defined html tag
	 * @param htmlTag
	 * @param content
	 * @return
	 */
	public static String wrapStringWithHtmlTag(String htmlTag, String content) {
		return "<"+htmlTag+">"+content+"</"+htmlTag+">";
	}
	
	public static String toStemmed(String s) {
		String stemmedContent = "";
		Stemmer stemmer = new Stemmer();
		for (String t : s.split("\\s+")) {
			stemmer.add(t.toCharArray(), t.length());
			stemmer.stem();
			if (stemmedContent.equals("")) {
				stemmedContent += stemmer.toString();
			} else {
				stemmedContent += " " + stemmer.toString();
			}
		}
		return stemmedContent;
	}
}
