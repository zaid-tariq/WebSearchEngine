package com.example.main.backend;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.example.main.backend.config.DBConfig;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

			HTMLDocument doc = Indexer.index(urlToCrawl);
			con = this.db.getConnection();
			if (doc != null) {
				con.setAutoCommit(false);

				db.insertDocDataToDatabase(doc, con);
				db.insertURLSToQueue(doc.getLinks(), depth, con);
				db.insertURLToVisited(doc.getUrl(), con);

				con.commit();
			}

		} catch (IOException | URISyntaxException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	
}
