package com.example.main.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.main.backend.api.SearchAPI;
import com.example.main.backend.api.responseObjects.SearchResultResponse;


@RestController
@RequestMapping("/rest/search")
public class SearchAPIController {
	
	@GetMapping("/conjunctive")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIconjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		System.out.println("Processing "+query);
		
		return ResponseEntity.ok().body(new SearchAPI().searchAPIconjunctive(query, limit));
		
	}
	
	@GetMapping("/disjunctive")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIdisjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		System.out.println("Processing "+query);
		
		SearchResultResponse res = new SearchAPI().searchAPIdisjunctive(query, limit);
		return ResponseEntity.ok().body(res);
	}
	
	
	@GetMapping("/updateScores")
	@ResponseBody
	public ResponseEntity<String> updateScores() {
		
		//TODO: Check when to call Tf IDf score update. Maybe after crawler is done? A trigger in DB or something her in java class for Crawler?
		
		new SearchAPI().updateScores();
		return ResponseEntity.ok().body("Scores updated!");
	}
	
	@RequestMapping("*")
	@ResponseBody
	public String fallbackMethod() {
		return "Page not found";
	}
}