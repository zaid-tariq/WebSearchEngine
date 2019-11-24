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
import com.example.main.backend.api.responseObjects.SearchResultResponse;


@RestController
@RequestMapping("/rest/search")
public class SearchAPIController {

	//TODO: Implement rate limitation
	//TODO: Restrict outside access to uni network
	//TODO: Implement site filter in search query. No requirement to do this in SQL in exercise, can do it in java too
	
	@GetMapping("/conjunctive")
	@ResponseBody
	public SearchResultResponse searchAPIconjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		System.out.println("Processing "+query);
		Connection con = null;
		SearchResultResponse res = new SearchResultResponse(query, limit);
		
		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			res = handler.searchConjunctiveQuery(con, query, limit, res);
			int cw = handler.getCollectionSize(con);
			res.setCollectionSize(cw);
			res = handler.getStats(con, query, res);
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
		
		return res;
	}
	
	@GetMapping("/disjunctive")
	@ResponseBody
	public SearchResultResponse searchAPIdisjunctive(@RequestParam(value = "query") String query,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		
		System.out.println("Processing "+limit);
		SearchResultResponse res = new SearchResultResponse(query, limit);
		Connection con = null;
		
		try {
			con = new DatabaseCreator().getConnection();
			DBHandler handler = new DBHandler();
			res = handler.searchDisjunctiveQuery(con, query, limit, res);
			int cw = handler.getCollectionSize(con);
			res.setCollectionSize(cw);
			res = handler.getStats(con, query, res);
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
		
		return res;
	}
	
	
	@GetMapping("/updateScores")
	@ResponseBody
	public String updateScores() {
		
		//TODO: Check when to call Tf IDf score update. Maybe after crawler is done?
		
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