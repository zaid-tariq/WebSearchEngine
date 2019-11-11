package backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class HTMLParser {

    public HTMLDocument parse(URL url) throws IOException {
        HTMLDocument doc = new HTMLDocument(url);

        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        //2. parse the file for links --> store links just temporary --> insert when end parsing
        //3. remove all tags
        //4. remove all stopwords

        return doc;
    }
}
