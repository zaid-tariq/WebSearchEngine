package com.example.main.backend.controller;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.api.SearchAPI;
import com.example.main.backend.api.responseObjects.SearchResultResponse;


@RestController
@RequestMapping("/rest/search")
public class SearchAPIController {

	//TODO: Implement rate limitation. Use Spring Boot feature.
	//TODO: Restrict outside access to uni network.
	//TODO: Implement site filter in search query. No requirement to do this in SQL in exercise, can do it in java too
	
	@GetMapping("/conjunctive")
	@ResponseBody
	public SearchResultResponse searchAPIconjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		System.out.println("Processing "+query);
		
		return new SearchAPI().searchAPIconjunctive(query, limit);
	}
	
	@GetMapping("/disjunctive")
	@ResponseBody
	public SearchResultResponse searchAPIdisjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		System.out.println("Processing "+query);
		
		return new SearchAPI().searchAPIdisjunctive(query, limit);
	}
	
	
	@GetMapping("/updateScores")
	@ResponseBody
	public String updateScores() {
		
		//TODO: Check when to call Tf IDf score update. Maybe after crawler is done? A trigger in DB or something her in java class for Crawler?
		
		Connection con = null;
		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			handler.computeTfIdf(con);
		}
		catch( SQLException ex) {
			ex.printStackTrace();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if(con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
		return "Scores updated!";
	}
	
	
	@RequestMapping("*")
	@ResponseBody
	public String fallbackMethod() {
		return "Page not found";
	}
}