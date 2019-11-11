package backend;

import java.net.URI;

public class CrawlerRunnable implements Runnable {

	private URI urlToCrawl;

	public CrawlerRunnable(URI urlToCrawl) {
		this.urlToCrawl = urlToCrawl;
	}

	@Override
	public void run() {
		Indexer.parse(urlToCrawl);
	}
}
