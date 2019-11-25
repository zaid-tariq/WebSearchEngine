package com.example.main.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.main.backend.api.SearchAPI;
import com.example.main.backend.api.responseObjects.SearchResultResponse;

@Controller
public class GetSearchResults {

	@RequestMapping("/")
	public String helloWorld(Model model) {
		return "index";
	}
	
	@RequestMapping("/results")
	public String results(Model model, @RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "20") int limit) {
		
		SearchResultResponse results = new SearchAPI().searchAPIdisjunctive(query, limit);
		model.addAttribute("results", results.resultList);
		return "results";
	}
}
