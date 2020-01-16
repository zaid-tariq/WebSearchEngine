package com.example.main.backend;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.main.backend.dao.Query;

public class QueryParser {
	
	public static final int TILDA_TERMS = 0;
	public static final int NON_TILDA_TERMS = 0;
	
	/*
	 * Unintelligent parser
	 * Precedence:
	 * 	1- Site operator
	 * 	2- Quotes
	 *  3- Tilda 
	 */

	public static Query resolveSiteOperator(String a_query) {

		Query q = new Query();
		a_query = a_query.trim();

		// That means, that a site operator is included in that query
		int indexOperator = a_query.indexOf("site:");
		if (indexOperator == -1) {
			q.query = a_query;
		} else if (indexOperator > 0) {
			// Split by site operator --> this is the easy way
			String[] tokens = a_query.split("site:");
			if (tokens.length == 2) {
				q.query = tokens[0];
				String site = tokens[1];
				try {
					URL url = new URL(site.trim());
					q.site = url;
				} catch (Exception ex) {
					System.out.println("Not a valid site. Operator ignored.");
				}
			}
		} else if (indexOperator == 0) {
			String[] tokens = a_query.split(" ");
			// Check if site operator and url are combined in one token
			String site = "";
			if (tokens[0].trim().length() > 5) {
				// Yes they are combined
				site = tokens[0].trim().substring(5);
				q.query = tokens[1];
			} else {
				// No there was a space between it
				site = tokens[1].trim();
				q.query = tokens[2];
			}
			// convert to url
			try {
				URL url = new URL("https", site, "/");
				q.site = url;
			} catch (Exception ex) {
				System.out.println("Not a valid site. Operator ignored.");
			}
		}
		return q;
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

	public static List<List<String>> getTermsWithoutQuotes(String query) {
		
		List<String> tildaTerms = new ArrayList<String>();
		List<String> nonTildaTerms = new ArrayList<String>();
		
		for (String subQuery : QueryParser.getTermsInQuotes("\"" + query + "\"")) {
			List<List<String>> resolvedTerms = QueryParser.resolveTildaOperator(subQuery);
			tildaTerms.addAll(resolvedTerms.get(TILDA_TERMS));
			nonTildaTerms.addAll(resolvedTerms.get(NON_TILDA_TERMS));
		}

		List<List<String>> ret = new ArrayList<List<String>>();
		ret.add(tildaTerms);
		ret.add(nonTildaTerms);
		return ret;
	}
	
	public static List<String> getTermsWithoutQuotes_2(String query) {
		
		List<List<String>> resolvedTerms = getTermsWithoutQuotes(query);
		List<String> combinedterms = new ArrayList<String>();
		combinedterms.addAll(resolvedTerms.get(TILDA_TERMS));
		combinedterms.addAll(resolvedTerms.get(NON_TILDA_TERMS));
		return combinedterms;
	}
	
	public static List<List<String>> resolveTildaOperator(String a_query) {
		List<String> tildaTerms = new ArrayList<String>();
		List<String> nonTildaTerms = new ArrayList<String>();
		String[] tilda_tokens = a_query.split("~");
		for(int i = 0; i < tilda_tokens.length; i++) {
			String sub_query = tilda_tokens[i].trim();
			if(sub_query.length() > 0) {
				String[] space_tokens = a_query.split("\\s+");
				for(int j = 0; j < space_tokens.length; j++) {
					String term = space_tokens[j].trim();
					if(j == 0 && tilda_tokens.length > 1)
						tildaTerms.add(term);
					else nonTildaTerms.add(term);						
				}
			}
		}
		
		List<List<String>> ret = new ArrayList<List<String>>();
		ret.add(tildaTerms);
		ret.add(nonTildaTerms);
		return ret;
	}
}
