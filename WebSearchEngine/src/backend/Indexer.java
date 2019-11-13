package backend;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Indexer {

	// 1. parse meta tag for text html; if none there just parse the file
	// 2. parse the file for links --> store links just temporary --> insert when
	// end parsing
	// 3. remove all tags
	// 4. remove all stopwords
	// 4. parse the text with stemming and counting of words

	public static void index(URI urlToIndex) throws IOException, URISyntaxException {
		HTMLParser htmlParser = new HTMLParser();
		HTMLDocument doc = htmlParser.parse(urlToIndex);

		writeDataToDatabase(doc);
	}

	public static void writeDataToDatabase(HTMLDocument doc) {
		// TODO: write doc stats to the database
	}
}
