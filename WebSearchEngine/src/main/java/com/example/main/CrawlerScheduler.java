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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.main.backend.Crawler;
import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.utils.Utils;

@Service
public class CrawlerScheduler implements CommandLineRunner {

	@Autowired
	DatabaseCreator dbc;

	@Autowired
	DBHandler dbh;
	
	@Autowired
	private ApplicationContext appContext;

	@Value("${crawler.max_docs}")
	int max_docs;

	@Value("${crawler.max_depth}")
	int max_depth;

	@Value("${crawler.leave_domain_boolean}")
	boolean leave_domain_boolean;

	@Value("${crawler.numberOfThreadsToSpawn}")
	int numberOfThreadsToSpawn;

	@Override
	public void run(String... args) {
		try {
			dbc.create();

			
			CrawlerTask task = new CrawlerTask();
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(task, 1000, TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
			System.out.println("Scheduled crawler!");
//			
//			System.out.println("Crawler started");
//			System.out.println("Following commands are provided:");
//			System.out.println("stop - stops the crawler");
//			System.out.println("restart - restarts the crawler");
//			System.out.println("stop completely - stops the crawler and terminates this command line");
//			Scanner reader = new Scanner(System.in);
//			boolean wait = true;
//			while (wait) {
//				String input = reader.nextLine();
//				switch (input.toLowerCase()) {
//				case "stop":
//					dbh.setCrawlerFlag(false);
//					System.out.println("Stopped the crawler");
//					break;
//				case "restart":
//					startCrawler();
//					System.out.println("Restarted the crawler");
//					break;
//				case "stop completely":
//					System.out.println("Terminated");
//					dbh.setCrawlerFlag(false);
//					wait = false;
//					break;
//				default:
//					break;
//				}
//			}
//			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void startCrawler() throws IOException, SQLException {
		File file = null;
		try {
			file = new ClassPathResource("seed_urls.txt").getFile();
		} catch (FileNotFoundException ex) {
			file = Utils.createTempFileFromInputStream("seed_urls.txt");
		}

		Set<URL> urls = new HashSet<URL>();
		BufferedReader r = new BufferedReader(new FileReader(file));

		String word;
		while ((word = r.readLine()) != null) {
			try {
				urls.add(new URL(word.trim()));
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			}
		}
		r.close();
		Crawler crawl = new Crawler();
		AutowireCapableBeanFactory factory = appContext.getAutowireCapableBeanFactory();
		factory.autowireBean(crawl);
		crawl.init(urls, max_depth, max_docs, leave_domain_boolean, numberOfThreadsToSpawn);
		factory.initializeBean(crawl, "runnable");
		crawl.start();
	}
	
	private class CrawlerTask extends TimerTask{

		@Override
		public void run() {
			try {
				startCrawler();
			} catch (IOException | SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
}