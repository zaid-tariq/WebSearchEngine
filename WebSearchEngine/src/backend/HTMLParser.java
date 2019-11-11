package backend;

import java.io.IOException;
import java.net.URI;

public class HTMLParser {

	public HTMLDocument parse(URI url) throws IOException {
		HTMLDocument doc = new HTMLDocument(url);

		// 2. parse the file for links --> store links just temporary --> insert when
		// end parsing
		// 3. remove all tags
		// 4. remove all stopwords

		return doc;
	}
}
