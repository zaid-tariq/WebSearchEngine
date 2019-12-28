package com.example.main.backend.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;

public class Utils {
	
	public static File createTempFileFromInputStream(String a_resourceName) throws IOException {
		
		File tempFile = File.createTempFile(a_resourceName, null);
		tempFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempFile);
		IOUtils.copy( new ClassPathResource(a_resourceName).getInputStream(), out);
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

}
