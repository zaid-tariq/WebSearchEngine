package com.example.main.backend.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.SearchAPI;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.example.main.backend.dao.AdForm;
import com.example.main.backend.utils.DocumentSimilarity;

@RestController
public class SearchAPIController {

	@Autowired
	SearchAPI searchApi;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private DBHandler db;

	@GetMapping("/is-project/conjunctive")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIconjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "k", defaultValue = "50") int limit) {

		return ResponseEntity.ok()
				.body(searchApi.searchAPIconjunctive(query, limit, new String[] { "english", "german" }));

	}

	@GetMapping("/is-project/json")
	@ResponseBody
	public ResponseEntity<SearchResultResponse> searchAPIdisjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "k", defaultValue = "20") int limit,
			@RequestParam(value = "score", defaultValue = "3") int scoringMethod) {

		// TODO: Insert language flag
		SearchResultResponse res = searchApi.searchAPIdisjunctive(query, limit, new String[] { "english" },
				scoringMethod, SearchAPI.DOCUMENT_MODE);
		return ResponseEntity.ok().body(res);
	}

	@RequestMapping(value = "/is-project/ad-add", method = RequestMethod.POST)
	public void addAd(@ModelAttribute("adForm") AdForm form, HttpServletResponse response) throws IOException {
		// Convert String
		String ngrams = form.getNgrams().replace("{", "").replace("}", "");
		String[] grams = ngrams.replace("[","").replace("]","").split("\\s.,\\s.");

		try {
			db.insertAd(form.getUrl(), form.getImageURL(), form.getDescription(), grams, form.getPricePerClick(),
					form.getTotalBudget());
			response.sendRedirect("/is-project/ads.html");
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendRedirect("/is-project/ads.html");
		}
	}

	@RequestMapping(value = "/is-project/clickedAd", method = RequestMethod.GET)
	public void clickedAd(@RequestParam(value = "id") int id) {
		try {
			db.incrementAdClicks(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@RequestMapping("*")
	@ResponseBody
	public String fallbackMethod() {
		return "Page not found";
	}

	@RequestMapping("/getErrors")
	@ResponseBody
	public ResponseEntity<String> getErrors() throws SQLException {

		DBHandler db = new DBHandler();
		AutowireCapableBeanFactory factory = appContext.getAutowireCapableBeanFactory();
		factory.autowireBean(db);
		// DocumentSimilarity.updateJaccardTables(db);
		return ResponseEntity.ok().body(DocumentSimilarity.printErrors(db));

	}
}