package com.example.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.main.backend.DBHandler;
import com.example.main.backend.api.responseObjects.SearchResultResponse;

@Profile("cli")
@Component
public class CLI implements CommandLineRunner {

	@Autowired
	DBHandler handler;

	@Override
	public void run(String... args) {
		// @query, @k, @typeOfSearch
		// The params are shiftet because of the profile command
		try {
			SearchResultResponse res = null;
			if (args[3].trim().equals("conjunctive")) {
				String queryConjunctive = "\"" + args[1].trim() + "\"";
				res = handler.searchConjunctiveQuery(queryConjunctive, Integer.parseInt(args[2].trim()),
						new String[] { "english", "german" }, null);
			} else if (args[3].trim().equals("disjunctive"))
				res = handler.searchDisjunctiveQuery(args[1].trim(), Integer.parseInt(args[2].trim()),
						new String[] { "english", "german" }, null, 3);
			else
				throw new Exception("Choose either conjunctive or disjunctive query method");

			if (res.getResultList().size() > 0) {
				res.printResult();
			} else {
				System.out.println("No results for your query :(");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}