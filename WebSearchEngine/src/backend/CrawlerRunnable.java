package backend;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class CrawlerRunnable implements Runnable {

	private URI urlToCrawl;

	public CrawlerRunnable(URI urlToCrawl) {
		this.urlToCrawl = urlToCrawl;
	}

	@Override
	public void run() {
		try {
			Indexer.index(urlToCrawl);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
