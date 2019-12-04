package com.example.main.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
	
	@Autowired
	SearchAPI searchApi;
	
	@GetMapping("/conjunctive")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIconjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		
		return ResponseEntity.ok().body(searchApi.searchAPIconjunctive(query, limit));
		
	}
	
	@GetMapping("/disjunctive")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIdisjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		
		SearchResultResponse res = searchApi.searchAPIdisjunctive(query, limit);
		return ResponseEntity.ok().body(res);
	}
	
	
	@GetMapping("/updateScores")
	@ResponseBody
	public ResponseEntity<String> updateScores() {
		searchApi.updateScores();
		return ResponseEntity.ok().body("Scores updated!");
	}
	
	@RequestMapping("*")
	@ResponseBody
	public String fallbackMethod() {
		return "Page not found";
	}
}