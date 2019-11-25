package com.example.main;

import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.example.main.backend.Crawler;
import com.example.main.backend.DatabaseCreator;


public class CLI {
	
	public static void main(String[] args) throws SQLException, URISyntaxException {
		
		DatabaseCreator db = new DatabaseCreator();
		java.sql.Connection connection = db.getConnection();
		
		try {
			//db.create();
//			DBHandler handler = new DBHandler();
//			//handler.computeTfIdf(connection);
//			handler.searchConjunctiveQuery(connection, "title2 title3 title4", 5);
//			handler.searchDisjunctiveQuery(connection, "title2 title3 title4", 5);
//			
			DatabaseCreator c = new DatabaseCreator();
			c.create();
			
			Set<URL> urls = new HashSet<URL>();
			urls.add(new URL("https://lerner.co.il/category/postgresql/"));
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