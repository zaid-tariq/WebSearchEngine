package com.example.main.backend.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;

import com.example.main.backend.dao.Snippet;

public class Utils {

	public static File createTempFileFromInputStream(String a_resourceName) throws IOException {

		File tempFile = File.createTempFile(a_resourceName, null);
		tempFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempFile);
		IOUtils.copy(new ClassPathResource(a_resourceName).getInputStream(), out);
		return tempFile;
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
	
	public static Map<String, Boolean> splitExpandedQuery(List<String> expandedQueryTerms) {
		
		Map<String, Boolean> resolvedTerms = new HashMap<String, Boolean>();
		for(String term : expandedQueryTerms) {
			String[] split = term.split("=");
			resolvedTerms.put(split[0], true);
			if(split.length > 1) {
				String[] syns = split[1].split(":");
				for(String syn : syns) {
					resolvedTerms.put(syn, false);
				}
			}	
		}
		return resolvedTerms;
	}
	
	public static String stemTerm(String term) {
		Stemmer stemmer = new Stemmer();
		stemmer.add(term.toCharArray(), term.length());
		stemmer.stem();
		String stemmedWord = stemmer.toString();
		return stemmedWord;
	}
	
	public static String formatQueryStringForGetUrlRequest(String a_query) {
		
		String[] query_tokens = a_query.trim().split(" ");
		String formattedQueryTokens = query_tokens[0].trim();
		for(int i = 1; i < query_tokens.length; i++) 
			formattedQueryTokens += "+" + query_tokens[i].trim(); 
		
		return formattedQueryTokens;
	}
	
}
