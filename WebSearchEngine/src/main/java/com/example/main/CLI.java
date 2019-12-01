package com.example.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.responseObjects.SearchResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Component
public class CLI {
	@Autowired
	public static DBHandler handler;
	
	public static void main(String[] args) throws Exception {
	
		//@query, @k, @typeOfSearch
		
		if (args.length != 3) {
            throw new IllegalArgumentException("Invalid number of arguments");
        }
		
		SearchResultResponse res = null;
		 
		if(args[2].equals("conjunctive"))
			res = handler.searchConjunctiveQuery(args[0], Integer.parseInt(args[1]), null);
		else if(args[5].equals("disjunctive"))
			res = handler.searchDisjunctiveQuery(args[0], Integer.parseInt(args[1]), null);
		else
			throw new Exception("Choose either conjunctive or disjunctive query method");
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(res.resultList);
		System.out.println(json);
	}
}