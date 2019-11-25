package com.example.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;

import com.example.main.backend.Crawler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.config.DBConfig;


public class CrawlerScheduler {
	
	public static void main(String[] args) throws SQLException, URISyntaxException, IOException, InterruptedException {
		
		//@max_depth, @max_docs, @leaf_domain_boolean, @numberOfThreadsToSpawn
		
		new DBConfig();
		
		DatabaseCreator db = new DatabaseCreator();
		db.create();
		java.sql.Connection connection = db.getConnection();
		
		try {
			db.create();
			
			File file = new ClassPathResource("seed_urls.txt").getFile();
			Set<URL> urls = new HashSet<URL>();
			BufferedReader r = new BufferedReader(new FileReader(file));

			String word;
			while ((word = r.readLine()) != null) {
				System.out.println(word);
				try {
					urls.add(new URL(word.trim()));
				}
				catch(MalformedURLException ex) {
					ex.printStackTrace();
				}
			}
			r.close();
			Crawler crawl = new Crawler(urls, Integer.parseInt(args[0]), Integer.parseInt(args[1]),
					Boolean.parseBoolean(args[2]), Integer.parseInt(args[3]));
			crawl.start();
			System.out.println("STARTTTTT");

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