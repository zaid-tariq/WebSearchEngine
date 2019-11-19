package backend;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Indexer {

	public static HTMLDocument index(URI urlToIndex) throws IOException, URISyntaxException {
		HTMLParser htmlParser = new HTMLParser();
		HTMLDocument doc = htmlParser.parse(urlToIndex);
		return doc;
	}
}
