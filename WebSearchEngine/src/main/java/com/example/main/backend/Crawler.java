package com.example.main.backend;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.config.DBConfig;

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
	private ExecutorService exs;

	/**
	 * Creates a new crawler with the specified parameters. If there is already a configuration in the database, then the set parameters will be overridden.
	 * @param urls Starting URLs
	 * @param maximumDepth Maximum depth to crawl
	 * @param maximumNumberOfDocs Maximum number of documents to crawl
	 * @param leaveDomain Should the crawler also crawl documents of another domain
	 * @param parallelism Number of threads which are used to crawl the web
	 */
	public void init(Set<URL> urls, int maximumDepth, int maximumNumberOfDocs, boolean leaveDomain, int parallelism) {
		this.urls.addAll(urls);
		this.maximumDepth = maximumDepth;
		this.maximumNumberOfDocs = maximumNumberOfDocs;
		this.leaveDomain = leaveDomain;
		if (parallelism < 1) {
			throw new IllegalArgumentException("The number of threads cannot be smaller than 1");
		}
		this.parallelism = parallelism;

		// Get database connection
		try {
			// Insert starting URLs to the database queue table
			db.queueURLs(urls);
		} catch (SQLException | MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void start() {
		Connection loadCon = null;
		try {
			DBConfig conf = new DBConfig();
			loadCon = DriverManager.getConnection(conf.getUrl(), conf.getUsername(), conf.getPassword());
			PreparedStatement ps = loadCon.prepareStatement(
					"SELECT maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism FROM crawlerState");
			ps.execute();
			ResultSet res = ps.getResultSet();
			if (res.next()) {
				// Only if there is a valid configuration stored in the database
				this.maximumDepth = res.getInt(1);
				this.maximumNumberOfDocs = res.getInt(2);
				this.crawledDocuments = res.getInt(3);
				this.leaveDomain = res.getBoolean(4);
				this.parallelism = res.getInt(5);
			}
			res.close();
			ps.close();
			
			exs = Executors.newFixedThreadPool(parallelism);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (loadCon != null) {
				try {
					loadCon.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		super.start();
	}

	@Override
	public void run() {
		try {
			while (crawl) {
				if (maximumNumberOfDocs != crawledDocuments) {
					Object[] entry = db.getNextURL();
					if (entry != null && maximumDepth != (int) entry[1]) {
						if (leaveDomain) {
							CrawlerRunnable runnable = new CrawlerRunnable();
							runnable.init((URL) entry[0], (int) entry[1]);
							exs.submit(runnable);
							crawledDocuments++;
						} else {
							boolean contains = false;
							for (URL u : urls) {
								if (u.getHost().equals(((URL) entry[0]).getHost())) {
									contains = true;
								}
							}
							if (contains) {
								CrawlerRunnable runnable = new CrawlerRunnable();
								runnable.init((URL) entry[0], (int) entry[1]);
								exs.submit(runnable);
								crawledDocuments++;
							}
						}
					}
				} else {
					// Reached maximum to crawl documents -> stop crawler
					db.cancel(maximumNumberOfDocs, maximumDepth, crawledDocuments, leaveDomain, parallelism);
				}
			}
		} catch (SQLException | URISyntaxException e) {
			e.printStackTrace();
		} 
	}

	
}
