package com.example.main.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.main.backend.api.SearchAPI;

@Controller
public class GetSearchResults {
	
	@Autowired
	SearchAPI api;

	@RequestMapping("/")
	public String helloWorld(Model model) {
		return "index";
	}
	
	@RequestMapping("/results")
	public String results(Model model, @RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "20") int limit, @RequestParam(value="lang") String languages) {
		
		model.addAttribute("results", api.searchAPIdisjunctive(query, limit, languages.split(" ")).resultList);
		model.addAttribute("didYouMean", api.getDidYouMeanQuery(query));
		return "results";
	}
}
