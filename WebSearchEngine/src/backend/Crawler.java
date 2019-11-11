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
			con = DriverManager.getConnection("");
			stmtNextURL = con.prepareStatement(""); // TODO: create statement

			// TODO: Insert starting URLs to the database queue table
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			int crawledDocuments = 0;
			while (crawl) {
				if (maximumNumberOfDocs != crawledDocuments) {
					URI urlToCrawl = getNextURL(leaveDomain);
					if (maximumDepth != currentMaxDepth) {
						exs.submit(new CrawlerRunnable(urlToCrawl));
						maximumNumberOfDocs++;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private URI getNextURL(boolean leaveDomain) throws SQLException, URISyntaxException {
		// TODO: set Parameters stmtNextURL.setString(parameterIndex, x);
		stmtNextURL.execute();
		ResultSet res = stmtNextURL.getResultSet();
		res.next();

		return new URI(res.getString(0));
	}
}
