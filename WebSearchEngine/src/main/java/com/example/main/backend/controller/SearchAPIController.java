package com.example.main.backend.controller;

import java.net.URISyntaxException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.main.CLIIndexing;
import com.example.main.CrawlerScheduler;
import com.example.main.backend.api.SearchAPI;
import com.example.main.backend.api.responseObjects.SearchResultResponse;


@RestController
public class SearchAPIController {
	
	@Autowired
	SearchAPI searchApi;
	
	@Autowired
	private ApplicationContext appContext;
	
	@GetMapping("/is-project/conjunctive")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIconjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "k", defaultValue = "50") int limit) {
		
		return ResponseEntity.ok().body(searchApi.searchAPIconjunctive(query, limit, new String[] {"english","german"}));
		
	}
	
	@GetMapping("/is-project/json")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIdisjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "k", defaultValue = "50") int limit, @RequestParam(value = "score") int scoringMethod) {
		
		//TODO: Insert language flag
		SearchResultResponse res = searchApi.searchAPIdisjunctive(query, limit, new String[] {"english"},scoringMethod, SearchAPI.DOCUMENT_MODE);
		return ResponseEntity.ok().body(res);
	}
	
	@RequestMapping("*")
	@ResponseBody
	public String fallbackMethod() {
		return "Page not found";
	}
	
	@RequestMapping("/run")
	public void runCrawlerIndexer() {
		 try {
			CLIIndexing indexer = new CLIIndexing();
			CrawlerScheduler crawler = new CrawlerScheduler();
			AutowireCapableBeanFactory factory = appContext.getAutowireCapableBeanFactory();
			factory.autowireBean(crawler);
			factory.autowireBean(indexer);
			 crawler.run();
			indexer.run();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}