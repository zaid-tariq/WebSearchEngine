package com.example.main.backend;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import org.apache.commons.text.StringEscapeUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.w3c.tidy.Tidy;

import com.example.main.backend.HTMLDocument.Language;
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

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Convert it to XHTML --> makes parsing easier
		Tidy tidy = new Tidy();
		tidy.setXmlTags(false);
		tidy.setXHTML(true);
		tidy.setMakeClean(true);
		tidy.setForceOutput(true);
		tidy.parse(urlToIndex.openStream(), out);

		byte[] b = out.toByteArray();
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(b)));

		String content = "";
		String line;
		while ((line = br.readLine()) != null) {
			// Add empty space before line to avoid merge of words
			content += " " + line;
		}
		br.close();

		// We don't want the content of that tags as they are unnecessary for us
		content = content.replaceAll("<script(.*?)>(.*?)</script>", " ");
		content = content.replaceAll("<style(.*?)>(.*?)</style>", " ");

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

		// 2. parse the file for links and images

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
		
		System.out.println("STEP 1");
		Matcher mImages = Pattern.compile("<img[^>]*src=\"(.*?)\"[^>]*>").matcher(content);
		while (mImages.find()) {
			try {
				List<String> urls = UrlExtractor.extractUrls(mImages.group(1));
				for (int x=0;x<urls.size();x++) {
					doc.addImage(new URL(urls.get(x)),x);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		System.out.println("STEP 2");
		String specialChars = "(<br>|\\?|\\!|\\>|\\/|&nbsp;|&bull;|&amp;|&#39;|&hellip;|\\.|\\,|\\:|\\;|\\[|\\]|\\{|\\}|\\||\\+|\\-|\\*|\\)|\\(|\\=|\\\"|\\|'|'|&|…|#|_)";
		// That pattern matches every tag except the image tag:
		// (?!<img[^>]*src=\"(.*?)\"[^>]*>)(<[^>]*>)
		System.out.println("STEP 2.1");
		String imageText = content.replaceAll("(?!<img[^>]*src=\"(.*?)\"[^>]*>)(<[^>]*>)", " ");
		System.out.println("STEP 2.2");
		System.out.println(imageText);
		//TODO: find error here
		imageText = imageText.replaceAll("<img.*>", "$img$");
		System.out.println("STEP 2.3");
		imageText = imageText.replaceAll(specialChars, " ");

		/**
		 * Extract minimum distance to an image for a term in a range of 15 words around the image
		 */
		System.out.println("STEP 3");
		String[] imageContentTerms = imageText.split("\\s+");
		int imgNumber =0;
		for (int x = 0; x < imageContentTerms.length; x++) {
			if (imageContentTerms[x].equals("$img$")) {
				int tagPos = x;
				int leftOffset = 0;
				int rightOffset = 0;
				for (int dist = 1; dist <= 15; dist++) {
					// Jump over multiple img tags in a row
					int leftWordPos = tagPos - (dist + leftOffset);
					int rightWordPos = tagPos + dist + rightOffset;

					if (leftWordPos >= 0) {
						while (imageContentTerms[leftWordPos].equals("$img$")) {
							leftOffset++;
						}
						leftWordPos = tagPos - (dist + leftOffset);
						if (leftWordPos >= 0) {
							String word = imageContentTerms[leftWordPos];
							if (doc.getLanguage().equals(Language.ENGLISH)) {
								word = Utils.toStemmed(word);
							}
							doc.addTermDistance(doc.getImages().get(imgNumber).toString(),word, dist);
						}
					}

					if (rightWordPos >= imageContentTerms.length) {
						while (imageContentTerms[rightWordPos].equals("$img$")) {
							rightOffset++;
						}
						rightWordPos = tagPos + dist + rightOffset;

						if (rightWordPos < imageContentTerms.length) {
							String word = imageContentTerms[rightWordPos];
							if (doc.getLanguage().equals(Language.ENGLISH)) {
								word = Utils.toStemmed(word);
							}
							doc.addTermDistance(doc.getImages().get(imgNumber).toString(),word, dist);
						}
					}
				}
				imgNumber++;
			}
		}
		System.out.println("STEP 4");
		
		// 3. remove all tags
		LinkedList<String> extractedContent = new LinkedList<String>();

		content = content.replaceAll("<[^>]*>", " ");
		// Html Entities unescape
		content = StringEscapeUtils.builder(StringEscapeUtils.UNESCAPE_HTML4).escape(content).toString();
		for (String word : content.replaceAll(specialChars, " ").split("\\s+")) {
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
				}
			}
		} else {
			while (!words.isEmpty()) {
				String word = words.remove().toLowerCase();
				doc.incrementTermFrequency(word);
			}
		}

		doc.setContent(content.replaceAll("<[^>]*>", " "));

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
