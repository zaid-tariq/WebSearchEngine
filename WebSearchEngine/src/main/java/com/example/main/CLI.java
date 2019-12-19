package com.example.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.responseObjects.SearchResultResponse;


@Profile("cli")
@Component
public class CLI implements CommandLineRunner{
	
	@Autowired
	DBHandler handler;

	
	@Override
	public void run(String...args) {
		// @query, @k, @typeOfSearch
		
		System.out.println(handler == null);
			
		try {
			SearchResultResponse res = null;
			if(args[2].equals("conjunctive"))
				res = handler.searchConjunctiveQuery(args[0], Integer.parseInt(args[1]), new String[] {"english","german"}, null);
			else if(args[2].equals("disjunctive"))
				res = handler.searchDisjunctiveQuery(args[0], Integer.parseInt(args[1]),  new String[] {"english","german"}, null, 3);
			else
				throw new Exception("Choose either conjunctive or disjunctive query method");
			
			res.printResult();	
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}