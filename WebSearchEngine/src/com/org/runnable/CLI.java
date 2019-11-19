package com.org.runnable;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import backend.Crawler;
import backend.DBHandler;
import backend.DatabaseCreator;

public class CLI {
	
	public static void main(String[] args) throws SQLException, URISyntaxException {
		
		DatabaseCreator db = new DatabaseCreator();
		java.sql.Connection connection = db.getConnection();
		
		try {
			db.create();
//			DBHandler handler = new DBHandler();
//			//handler.computeTfIdf(connection);
//			handler.searchConjunctiveQuery(connection, "title2 title3 title4", 5);
//			handler.searchDisjunctiveQuery(connection, "title2 title3 title4", 5);
//			
			Set<URI> urls = new HashSet<URI>();
			urls.add(new URI("https://lerner.co.il/category/postgresql/"));
			Crawler crawl = new Crawler(urls, 2, -1, true, 5);
			crawl.start();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

}
