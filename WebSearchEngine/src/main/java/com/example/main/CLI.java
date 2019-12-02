package com.example.main;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import com.example.main.backend.DBHandler;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.api.responseObjects.SearchResultResponse;


public class CLI {
	
	public static void main(String[] args) throws SQLException, URISyntaxException, IOException, InterruptedException {
	
		//@jdbc url, @user, @pass, @query, @k, @typeOfSearch
		
		DatabaseCreator db = new DatabaseCreator(args[0], args[1], args[2]);
		db.create();
		java.sql.Connection connection = db.getConnection();
		
		try {
			db.create();
			DBHandler handler = new DBHandler();
			SearchResultResponse res = null;
			if(args[5].equals("conjunctive"))
				res = handler.searchConjunctiveQuery(connection, args[3], Integer.parseInt(args[4]), null);
			else if(args[5].equals("disjunctive"))
				res = handler.searchDisjunctiveQuery(connection, args[3], Integer.parseInt(args[4]), null);
			else
				throw new Exception("Choose either conjunctive or disjunctive query method");
			res.printResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}	
	}
}