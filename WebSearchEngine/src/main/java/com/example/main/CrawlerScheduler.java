package com.example.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.main.backend.Crawler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.utils.Utils;

@Service
public class CrawlerScheduler implements CommandLineRunner {
	
	@Autowired
	DatabaseCreator dbc;
	
	@Autowired
	Crawler crawl;
	
	@Value("${crawler.max_docs}")
	int max_docs;
	
	@Value("${crawler.max_depth}")
	int max_depth;
	
	@Value("${crawler.leaf_domain_boolean}")
	boolean leaf_domain;
	
	@Value("${crawler.numberOfThreadsToSpawn}")
	int numOfThreads;
	
	
	@Override
	public void run(String... args) throws SQLException, URISyntaxException, IOException, InterruptedException {
		
		try {
			dbc.create();
			
			File file = null;
			try{
				file = new ClassPathResource("seed_urls.txt").getFile();
			}
			catch(FileNotFoundException ex) {
				file = Utils.createTempFileFromInputStream("seed_urls.txt");
			}
			
			Set<URL> urls = new HashSet<URL>();
			BufferedReader r = new BufferedReader(new FileReader(file));

			String word;
			while ((word = r.readLine()) != null) {
				try {
					urls.add(new URL(word.trim()));
				}
				catch(MalformedURLException ex) {
					ex.printStackTrace();
				}
			}
			r.close();
			crawl.init(urls, max_depth, max_docs, leaf_domain, numOfThreads);
			crawl.start();
			System.out.println("Crawler Started");

		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}