package com.example.main.backend;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Crawler extends Thread {

	@Autowired
	DBHandler db;

	private Queue<URL> urls = new LinkedList<>();

	// If this parameter is set to -1 the crawler will go for infinite depth
	private int maximumDepth;

	// If this parameter is set to -1 the crawler will process data infinite long
	private int maximumNumberOfDocs;
	private boolean leaveDomain;
	private int crawledDocuments = 0;
	private int parallelism;
	private boolean crawl = true;
	private PreparedStatement stmtNextURL;
	private ExecutorService exs;

	public Crawler restore(int id) {

		try {
			return db.restoreCrawler();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void init(Set<URL> urls, int maximumDepth, int maximumNumberOfDocs, boolean leaveDomain, int parallelism) {
		this.urls.addAll(urls);
		this.maximumDepth = maximumDepth;
		this.maximumNumberOfDocs = maximumNumberOfDocs;
		this.leaveDomain = leaveDomain;
		if (parallelism < 1) {
			throw new IllegalArgumentException("The number of threads cannot be smaller than 1");
		}
		this.parallelism = parallelism;
		exs = Executors.newFixedThreadPool(parallelism);

		try {
			stmtNextURL = db.getPreparedStatement("SELECT * FROM crawlerQueue ORDER BY id FETCH FIRST ROW ONLY");
			// Insert starting URLs to the database queue table
			db.queueURLs(urls);
		} catch (SQLException | MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while (crawl) {
				if (maximumNumberOfDocs != crawledDocuments) {
					Object[] entry = db.getNextURL(this.stmtNextURL);
					if (entry != null && maximumDepth != (int) entry[1]) {
						if (leaveDomain) {
							
							CrawlerRunnable cr = new CrawlerRunnable();
							cr.init((URL) entry[0], (int) entry[1]);
							exs.submit(cr);
							setCrawledDocuments(crawledDocuments + 1);
						} else {
							boolean contains = false;
							for (URL u : urls) {
								if (u.getHost().equals(((URL) entry[0]).getHost())) {
									contains = true;
								}
							}
							if (contains) {
								CrawlerRunnable cr = new CrawlerRunnable();
								cr.init((URL) entry[0], (int) entry[1]);
								exs.submit(cr);
								exs.submit(cr);
								setCrawledDocuments(crawledDocuments + 1);
							}
						}
					}
				} else {
					// Reached maximum to crawl documents -> stop crawler
					cancel();
				}
			}
		} catch (SQLException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public int cancel() {
		crawl = false;
		return db.cancel(maximumDepth, maximumNumberOfDocs, crawledDocuments, leaveDomain, parallelism);
	}

	public void setCrawledDocuments(int crawledDocuments) {
		this.crawledDocuments = crawledDocuments;
	}

}
