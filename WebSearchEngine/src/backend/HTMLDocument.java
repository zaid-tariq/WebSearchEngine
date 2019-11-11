package backend;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class HTMLDocument {

    private URL url;
    private LinkedHashSet<URL> links = new LinkedHashSet<>();
    private HashMap<String, Integer> termFrequencies = new HashMap<>();

    public HTMLDocument(URL url) {
        this.url = url;
    }

    public void addLink(URL url) {
        links.add(url);
    }

    public void incrementTermFrequency(String term) {
        termFrequencies.computeIfPresent(term, (k, v) -> {
            v = v + 1;
            return v;
        });
        termFrequencies.putIfAbsent(term, 1);
    }

    public URL getUrl() {
        return url;
    }

    public LinkedHashSet<URL> getLinks() {
        return links;
    }

    public HashMap<String, Integer> getTermFrequencies() {
        return termFrequencies;
    }
}
