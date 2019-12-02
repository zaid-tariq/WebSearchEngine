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

import com.example.main.backend.config.DBConfig;

public class Crawler extends Thread {

	private Queue<URL> urls = new LinkedList<>();

	// If this parameter is set to -1 the crawler will go for infinite depth
	private int maximumDepth;

	// If this parameter is set to -1 the crawler will process data infinite long
	private int maximumNumberOfDocs;
	private boolean leaveDomain;
	private int crawledDocuments = 0;
	private int parallelism;

	private boolean crawl = true;
	private Connection con;
	private PreparedStatement stmtNextURL;
	private ExecutorService exs;

	/**
	 * Creates a new crawler with the specified parameters. If there is already a configuration in the database, then the set parameters will be overridden.
	 * @param urls Starting URLs
	 * @param maximumDepth Maximum depth to crawl
	 * @param maximumNumberOfDocs Maximum number of documents to crawl
	 * @param leaveDomain Should the crawler also crawl documents of another domain
	 * @param parallelism Number of threads which are used to crawl the web
	 */
	public Crawler(Set<URL> urls, int maximumDepth, int maximumNumberOfDocs, boolean leaveDomain, int parallelism) {
		super("crawler");
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
			DBConfig conf = new DBConfig();
			con = DriverManager.getConnection(conf.getUrl(), conf.getUsername(), conf.getPassword());
			stmtNextURL = con.prepareStatement("SELECT * FROM crawlerQueue ORDER BY id FETCH FIRST ROW ONLY");
			// Insert starting URLs to the database queue table
			queueURLs(urls, con);
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
					Object[] entry = getNextURL();
					if (entry != null && maximumDepth != (int) entry[1]) {
						if (leaveDomain) {
							System.out.println("Next document");
							exs.submit(new CrawlerRunnable((URL) entry[0], (int) entry[1]));
							crawledDocuments++;
						} else {
							boolean contains = false;
							for (URL u : urls) {
								if (u.getHost().equals(((URL) entry[0]).getHost())) {
									contains = true;
								}
							}
							if (contains) {
								exs.submit(new CrawlerRunnable((URL) entry[0], (int) entry[1]));
								crawledDocuments++;
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
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save current state of the crawler into the database
	 */
	public void cancel() {
		crawl = false;
		try {
			PreparedStatement clearCrawlerState = con.prepareStatement("TRUNCATE TABLE crawlerState RESTART IDENTITY");
			clearCrawlerState.execute();
			clearCrawlerState.close();

			PreparedStatement ps = con.prepareStatement(
					"INSERT INTO crawlerState (maximum_depth, maximum_docs, crawled_docs, leave_domain, parallelism) VALUES (?,?,?,?,?)");
			ps.setInt(1, maximumDepth);
			ps.setInt(2, maximumNumberOfDocs);
			ps.setInt(3, crawledDocuments);
			ps.setBoolean(4, leaveDomain);
			ps.setInt(5, parallelism);

			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the next URL from the database
	 * 
	 * @return null if there are no more URLs in the database, otherwise the URL and
	 *         the depth of that URL in the hierarchy
	 * @throws SQLException
	 * @throws URISyntaxException
	 */
	private Object[] getNextURL() throws SQLException, URISyntaxException {
		stmtNextURL.execute();
		ResultSet res = stmtNextURL.getResultSet();
		if (res.next()) {
			PreparedStatement stmnt = this.con.prepareStatement("DELETE FROM crawlerQueue WHERE id = ?");
			stmnt.setInt(1, res.getInt(1));
			stmnt.execute();
			stmnt.close();
			try {
				return new Object[] { new URL(res.getString(2)), res.getInt(3) };
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return null;

	}

	/**
	 * Inserts the provided URLs into the database if they are not already listed
	 * 
	 * @param urls URLs to insert
	 * @param con  Connection to the database
	 * @throws SQLException
	 * @throws MalformedURLException
	 */
	private void queueURLs(Set<URL> urls, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmtCheckIfExists = con
				.prepareStatement("SELECT count(url) FROM documents WHERE url LIKE ? GROUP BY url" + "	UNION "
						+ "SELECT count(url) FROM crawlerQueue WHERE url LIKE ? GROUP BY url");

		// Check if URL is already crawled. If so --> don't process it further
		// Reduces the number of accesses to a single domain

		Set<URL> urlsAlreadyCrawled = new HashSet<URL>();
		for (URL url : urls) {
			stmtCheckIfExists.setString(1, url.toString());
			stmtCheckIfExists.setString(2, url.toString());

			stmtCheckIfExists.execute();
			ResultSet s = stmtCheckIfExists.getResultSet();
			while (s.next()) {
				if (s.getInt(1) > 0) {
					urlsAlreadyCrawled.add(url);
				}
			}
		}

		for (URL url : urlsAlreadyCrawled) {
			urls.remove(url);
		}

		PreparedStatement stmtQueueURLs = con
				.prepareStatement("INSERT INTO crawlerQueue(id, url, current_depth) VALUES (DEFAULT, ?, ?)");

		for (URL url : urls) {
			stmtQueueURLs.setString(1, url.toString());
			stmtQueueURLs.setInt(2, 0);
			stmtQueueURLs.addBatch();
		}

		stmtQueueURLs.executeBatch();

		// If conflict on unique constraint url occurs --> ignore conflict and do
		// nothing
		PreparedStatement stmt = con.prepareStatement(
				"INSERT INTO documents (docid, url,crawled_on_date, language) VALUES (DEFAULT,?,NULL,NULL) ON CONFLICT DO NOTHING");

		for (URL url : urls) {
			stmt.setString(1, url.toString());
			stmt.addBatch();
		}

		stmt.executeBatch();
	}
}
