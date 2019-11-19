package backend;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Crawler extends Thread {

	private Queue<URI> urls = new LinkedList<>();
	private int maximumDepth;

	// If this parameter is set to -1 the crawler will process data infinite long
	private int maximumNumberOfDocs;
	private boolean leaveDomain;

	private boolean crawl = true;
	private Connection con;
	private PreparedStatement stmtNextURL;
	private ExecutorService exs;

	public Crawler(Set<URI> urls, int maximumDepth, int maximumNumberOfDocs, boolean leaveDomain, int parallelism) {
		this.urls.addAll(urls);
		this.maximumDepth = maximumDepth;
		this.maximumNumberOfDocs = maximumNumberOfDocs;
		this.leaveDomain = leaveDomain;
		if (parallelism < 1) {
			throw new IllegalArgumentException("The number of threads cannot be smaller than 1");
		}
		exs = Executors.newFixedThreadPool(parallelism);

		// Get database connection
		try {
			con = DriverManager.getConnection("jdbc:postgresql:project", "app", "pass");
			stmtNextURL = con.prepareStatement("SELECT * FROM crawlerQueue ORDER BY id FETCH FIRST ROW ONLY");
			// Insert starting URLs to the database queue table
			queueURLs(urls, con);
		} catch (SQLException | MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			int crawledDocuments = 0;
			while (crawl) {
				if (maximumNumberOfDocs != crawledDocuments) {
					Object[] entry = getNextURL(leaveDomain);
					if (entry != null && maximumDepth != (int) entry[1]) {
						if (leaveDomain) {
							exs.submit(new CrawlerRunnable((URI) entry[0]));
							crawledDocuments++;
						} else {
							boolean contains = false;
							for (URI u : urls) {
								if (u.getHost().equals(((URI) entry[0]).getHost())) {
									contains = true;
								}
							}
							if (contains) {
								exs.submit(new CrawlerRunnable((URI) entry[0]));
								crawledDocuments++;
							}
						}
					}
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

	public void cancel() {
		crawl = false;
		// Save current status into a database table
		try {
			con.prepareStatement("INSERT INTO ...");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private Object[] getNextURL(boolean leaveDomain) throws SQLException, URISyntaxException {
		stmtNextURL.execute();
		ResultSet res = stmtNextURL.getResultSet();
		if (res.next()) {
			System.out.println("yes");
			PreparedStatement stmnt = this.con.prepareStatement("DELETE FROM crawlerQueue WHERE id = ?");
			stmnt.setInt(1, res.getInt(1));
			stmnt.execute();
			stmnt.close();
			return new Object[] { new URI(res.getString(2)), res.getInt(3) };
		}
		return null;

	}

	private void queueURLs(Set<URI> urls, Connection con) throws SQLException, MalformedURLException {
		PreparedStatement stmtQueueURLs = con
				.prepareStatement("INSERT INTO crawlerQueue(url, current_depth) VALUES (?,?)");
		for (URI uri : urls) {
			stmtQueueURLs.setString(1, uri.toURL().toString());
			stmtQueueURLs.setInt(2, 0);
			stmtQueueURLs.addBatch();
		}

		stmtQueueURLs.executeBatch();
	}
}
