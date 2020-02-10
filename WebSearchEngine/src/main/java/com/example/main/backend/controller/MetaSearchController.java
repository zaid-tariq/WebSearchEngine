package com.example.main.backend.controller;

import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.main.backend.api.MetaSearchEngine;

@Controller
public class MetaSearchController {

	@Autowired
	MetaSearchEngine metaSearchApi;
	
	@RequestMapping("is-project/metasearch")
	public String metaSearch(Model model) {
		return "metasearch";
	}

	@RequestMapping("is-project/metasearch/config")
	public String metaSearchConfigAction(Model model, @RequestParam(value = "url", defaultValue = "") String url,
			@RequestParam(value = "action", defaultValue = "") String action) {
		
		if(url != null && url.length() > 0)
			metaSearchApi.dispatch_config_action(url, action);
		model.addAttribute("results", metaSearchApi.dispatch_config_action(null, "load"));
		return "metasearch_config";
	}
	
	@RequestMapping("is-project/metasearch/results")
	public String metaSearchConfigAction(Model model, @RequestParam(value = "query") String query,
			@RequestParam(value = "cutoff", defaultValue = "10") int cutoff) throws SQLException {

		if(cutoff <= 0)
			cutoff = 1;
		model.addAttribute("response", metaSearchApi.getSearchResults(query, cutoff));
		return "metasearch_results";
	}
	
}
