package backend;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class HTMLDocument {

    private URI url;
    private LinkedHashSet<URI> links = new LinkedHashSet<>();
    private HashMap<String, Integer> termFrequencies = new HashMap<>();

    public HTMLDocument(URI url) {
        this.url = url;
    }

    public void addLink(URI url) {
        links.add(url);
    }

    public void incrementTermFrequency(String term) {
        termFrequencies.computeIfPresent(term, (k, v) -> {
            v = v + 1;
            return v;
        });
        termFrequencies.putIfAbsent(term, 1);
    }

    public URI getUrl() {
        return url;
    }

    public LinkedHashSet<URI> getLinks() {
        return links;
    }

    public HashMap<String, Integer> getTermFrequencies() {
        return termFrequencies;
    }
}
