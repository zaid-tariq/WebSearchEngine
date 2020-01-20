package com.example.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.main.backend.Crawler;
import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.utils.Utils;

@Profile("crawler")
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
			timer.scheduleAtFixedRate(task, 1000, TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
			System.out.println("Scheduled crawler!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void startCrawler() throws IOException, SQLException {
		File file = null;
		try {
			ClassPathResource classPathResource = new ClassPathResource("seed_urls.txt");

			InputStream inputStream = classPathResource.getInputStream();
			File somethingFile = File.createTempFile("test", ".txt");
			try {
				java.nio.file.Files.copy(inputStream, somethingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} finally {
			    IOUtils.closeQuietly(inputStream);
			}
			file = somethingFile;
		} catch (FileNotFoundException ex) {
			System.out.println("File not found");
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
				System.out.println("Crawler started");
			} catch (IOException | SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
}