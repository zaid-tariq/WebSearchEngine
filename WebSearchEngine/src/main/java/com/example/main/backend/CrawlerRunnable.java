package com.example.main.backend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.dao.HTMLDocument;

@Component
public class CrawlerRunnable implements Runnable {

	@Autowired
	DBHandler db;

	private URL urlToCrawl;

	private int depth;

	public void init(URL urlToCrawl, int parentDepth) {
		this.urlToCrawl = urlToCrawl;
		this.depth = parentDepth + 1;
	}

	@Override
	public void run() {
		Connection con = null;
		try {
			System.out.println("CRAWLING STARTED");

			HTMLDocument doc = Indexer.index(urlToCrawl);

			System.out.println("DOC CRAWLED");

			con = db.getConnection();
			if (doc != null) {
				con.setAutoCommit(false);

				db.insertDocDataToDatabase(doc, con);
				db.insertURLSToQueue(doc.getLinks(), depth, con);
				db.insertURLToVisited(doc.getUrl(), con);
				db.insertImageDataToDatabase(doc, con);
				con.commit();
			}
		} catch (IOException | URISyntaxException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
