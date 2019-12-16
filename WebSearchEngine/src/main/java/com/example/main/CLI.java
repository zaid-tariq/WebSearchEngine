package com.example.main;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.api.responseObjects.SearchResultResponse;


@Component
public class CLI {
	
	@Autowired
	public static DBHandler handler;
	
	public static void main(String[] args) throws Exception {
	
		// @query, @k, @typeOfSearch
		
		SearchResultResponse res = null;
		if(args[2].equals("conjunctive"))
			//TODO: Insert language flag here
			res = handler.searchConjunctiveQuery(args[0], Integer.parseInt(args[1]), null, null);
		else if(args[2].equals("disjunctive"))
			//TODO: Insert language flag here
			res = handler.searchDisjunctiveQuery(args[0], Integer.parseInt(args[1]), null, null);
		else
			throw new Exception("Choose either conjunctive or disjunctive query method");
			
		
		res.printResult();	
	}
}