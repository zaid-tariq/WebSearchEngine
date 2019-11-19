package backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {

	public List<String> stopwords;

	public HTMLParser() throws IOException {
		stopwords = getStopwordsFromFile(new File("TODO: Filepath"));
	}

	/**
	 * Parses a given HTML file defined by the URI
	 * 
	 * @param url URL to the HTML file
	 * @return HTMLDocument that represents the important stuff of that file
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public HTMLDocument parse(URI url) throws IOException, URISyntaxException {
		HTMLDocument doc = new HTMLDocument(url);

		BufferedReader br = new BufferedReader(new InputStreamReader(url.toURL().openStream()));

		String content = "";
		String line;
		while ((line = br.readLine()) != null) {
			content += line;
		}
		
		//TODO: parse file for meta data

		// 2. parse the file for links
		Matcher mLinks = Pattern.compile("href=\"(.*?)\"").matcher(content);
		while (mLinks.find()) {
			doc.addLink(new URI(mLinks.group().replace("href=\"", "").replace("\"", "")));
		}

		// 3. remove all tags
		Matcher mTags = Pattern.compile("<[^>]*>").matcher(content);
		content = mTags.replaceAll(" "); // by empty string to split the content

		// 4. remove all stopwords, stemming and term frequency calculation at the same
		// time for efficiency
		String processedContent = "";
		Stemmer stemmer = new Stemmer();
		Queue<String> words = new LinkedList<String>(Arrays.asList(content.split(" ")));
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
