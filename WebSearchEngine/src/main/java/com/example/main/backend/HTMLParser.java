package com.example.main.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;

import com.example.main.backend.utils.LanguageDetector;
import com.example.main.backend.utils.Utils;
import com.shekhargulati.urlcleaner.UrlExtractor;

public class HTMLParser {

	public List<String> stopwords;

	public HTMLParser() throws IOException {

		File file = null;
		try {
			ClassPathResource classPathResource = new ClassPathResource("stopwords.txt");

			InputStream inputStream = classPathResource.getInputStream();
			File somethingFile = File.createTempFile("test1", ".txt");
			try {
				java.nio.file.Files.copy(inputStream, somethingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} finally {
			    IOUtils.closeQuietly(inputStream);
			}
			file = somethingFile;
		} catch (FileNotFoundException ex) {
			file = Utils.createTempFileFromInputStream("stopwords.txt");
		}
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

		// Test if document is HTML Document
		HttpURLConnection con = (HttpURLConnection) urlToIndex.openConnection();
		con.connect();
		String mimeType = con.getContentType();
		con.disconnect();
		// The mimeType contains more than that information, therefore we need to check
		// with contains
		if (!mimeType.contains("text/html")) {
			return null;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(urlToIndex.openStream()));

		String content = "";
		String line;
		while ((line = br.readLine()) != null) {
			content += line;
		}

		// 1. parse file for meta data e.g. language
		// If the language is set then use that otherwise use the language detector
		Matcher mLanguage = Pattern.compile("<html[^>]*lang=\"(.*?)\"[^>]*>").matcher(content);
		if (mLanguage.find()) {
			// Language is set in the HTML document language tag
			String proposedLanguage = mLanguage.group(1).toLowerCase().replaceAll("-[a-z]*", "");
			switch (proposedLanguage) {
			case "en":
				doc.setLanguage(HTMLDocument.Language.ENGLISH);
				break;
			case "de":
				doc.setLanguage(HTMLDocument.Language.GERMAN);
				break;
			default:
				// Just for completeness if we ever change the default value
				doc.setLanguage(null);
			}
		}

		// 2. parse the file for links

		Matcher mLinks = Pattern.compile("<a[^>]*href=\"(.*?)\"[^>]*>").matcher(content);
		while (mLinks.find()) {
			try {
				List<String> urls = UrlExtractor.extractUrls(mLinks.group(1));
				for (String url : urls) {
					doc.addLink(new URL(url));
				}
			} catch (MalformedURLException e) {
				// Yeah it is no URL...
				e.printStackTrace();
			}

		}

		// 3. remove all tags
		LinkedList<String> extractedContent = new LinkedList<String>();
		String specialChars = "(\\?|\\!|\\>|\\/|&nbsp;|&bull;|&amp;|&#39;|\\.|\\,|\\:|\\;|\\[|\\]|\\{|\\}|\\||\\+|\\-|\\*|\\)|\\(|\\=|\\\"|\\|'|'|&|…|#|_)";
		for (String word : content.replaceAll("<[^>]*>", " ").replaceAll(specialChars, "").split("\\s+")) {
			if (!word.trim().equals("")) {
				extractedContent.add(word);
			}
		}

		// Language was not detected by language tag in first step
		if (doc.getLanguage() == null) {
			int tuningParameterWordsToConsider = 40;
			LanguageDetector detector = new LanguageDetector();
			List<String> wordsToConsider = extractedContent.subList(0,
					tuningParameterWordsToConsider < extractedContent.size() ? tuningParameterWordsToConsider - 1
							: extractedContent.size() - 1);
			doc.setLanguage(detector.detect(wordsToConsider));
		}
		
		// 4. remove all stopwords, stemming and term frequency calculation at the same
		// time for efficiency
		// no stemming for german documents
		String processedContent = "";
		Stemmer stemmer = new Stemmer();
		Queue<String> words = extractedContent;

		if (doc.getLanguage().equals(HTMLDocument.Language.ENGLISH)) {
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
		} else {
			while (!words.isEmpty()) {
				String word = words.remove().toLowerCase();
				doc.incrementTermFrequency(word);
				processedContent += word;
			}
		}

		doc.setContent(processedContent);
		br.close();
		
		return doc;
	}

	/**
	 * Turns the file which contains all stopwords into an list of stopwords
	 * 
	 * @param File that contains all stopwords
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
