package com.example.main.backend;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Crawler extends Thread {

	@Autowired
	DBHandler db;

	@Autowired
	private ApplicationContext appContext;

	private LinkedList<URL> urls = new LinkedList<>();

	// If this parameter is set to -1 the crawler will go for infinite depth
	private int maximumDepth;

	// If this parameter is set to -1 the crawler will process data infinite long
	private int maximumNumberOfDocs;
	private boolean leaveDomain;
	private int crawledDocuments = 0;
	private int parallelism;

	private ExecutorService exs;

	/**
	 * Creates a new crawler with the specified parameters. If there is already a
	 * configuration in the database, then the set parameters will be overridden.
	 * 
	 * @param urls                Starting URLs
	 * @param maximumDepth        Maximum depth to crawl
	 * @param maximumNumberOfDocs Maximum number of documents to crawl
	 * @param leaveDomain         Should the crawler also crawl documents of another
	 *                            domain
	 * @param parallelism         Number of threads which are used to crawl the web
	 * @throws SQLException
	 * @throws MalformedURLException
	 */
	public void init(Set<URL> urls, int maximumDepth, int maximumNumberOfDocs, boolean leaveDomain, int parallelism)
			throws SQLException, MalformedURLException {
		this.urls.addAll(urls);
		this.maximumDepth = maximumDepth;
		this.maximumNumberOfDocs = maximumNumberOfDocs;
		this.leaveDomain = leaveDomain;
		if (parallelism < 1) {
			throw new IllegalArgumentException("The number of threads cannot be smaller than 1");
		}
		this.parallelism = parallelism;

		if (db.firstStartupCrawler()) {
			db.queueURLs(new HashSet<URL>(urls));
			db.setCrawlerFlag(true);
		} else {
			db.setCrawlerFlag(true);
		}
		db.insertCrawlerStateIfNotExists(maximumDepth, maximumNumberOfDocs, 0, leaveDomain, parallelism, true,
				fromLinkedList(new ArrayList<URL>(urls)));
	}

	@Override
	public synchronized void start() {
		try {
			Object[] saveState = db.loadCrawlerState();
			if (saveState != null) {
				this.maximumDepth = (int) saveState[0];
				this.maximumNumberOfDocs = (int) saveState[1];
				this.crawledDocuments = 0; //(int) saveState[2];
				this.leaveDomain = (boolean) saveState[3];
				this.parallelism = (int) saveState[4];
				this.urls = fromStringArray((String[]) saveState[6]);
				exs = Executors.newFixedThreadPool(this.parallelism);
				System.out.println("Start crawler!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		super.start();
	}

	@Override
	public void run() {
		try {
			while (db.getCrawlerFlag()) {
				if (maximumNumberOfDocs != crawledDocuments) {
					Object[] entry = db.getNextURL();
					if (entry != null && maximumDepth != (int) entry[1]) {
						if (leaveDomain) {
							exs.submit(createRunnable((URL) entry[0], (int) entry[1]));
							crawledDocuments++;
						} else {
							boolean contains = false;
							for (URL u : urls) {
								if (u.getHost().equals(((URL) entry[0]).getHost())) {
									contains = true;
								}
							}
							if (contains) {
								exs.submit(createRunnable((URL) entry[0], (int) entry[1]));
								crawledDocuments++;
							}
						}
					}
				} else {
					db.setCrawlerFlag(false);
					System.out.println("Crawling finished!");
				}
				Thread.sleep(100);
			}

			db.saveCrawlerState(maximumDepth, maximumNumberOfDocs, crawledDocuments, leaveDomain, parallelism, false,
					fromLinkedList(this.urls));
		} catch (SQLException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private CrawlerRunnable createRunnable(URL url, int parentDepth) {
		CrawlerRunnable runnable = new CrawlerRunnable();
		runnable.init(url, parentDepth);
		AutowireCapableBeanFactory factory = appContext.getAutowireCapableBeanFactory();
		factory.autowireBean(runnable);
		factory.initializeBean(runnable, "runnable");
		return runnable;
	}

	private LinkedList<URL> fromStringArray(String[] urls) {
		LinkedList<URL> l = new LinkedList<URL>();
		for (String s : urls) {
			try {
				l.add(new URL(s));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return l;
	}

	private String[] fromLinkedList(List<URL> urls) {
		ArrayList<String> s = new ArrayList<String>();
		for (URL u : urls) {
			s.add(u.toString());
		}
		Object[] o = s.toArray();
		return Arrays.copyOf(o, o.length, String[].class);
	}
}
