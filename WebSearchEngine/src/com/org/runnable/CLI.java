package com.org.runnable;

import java.sql.SQLException;
import backend.DBHandler;
import backend.DatabaseCreator;

public class CLI {
	
	public static void main(String[] args) throws SQLException {
		
		DatabaseCreator db = new DatabaseCreator();
		java.sql.Connection connection = db.getConnection();
		
		try {
			db.create();
			DBHandler handler = new DBHandler();
			//handler.computeTfIdf(connection);
			handler.searchConjunctiveQuery(connection, "title2 title3 title4", 5);
			handler.searchDisjunctiveQuery(connection, "title2 title3 title4", 5);

		} catch (SQLException e) {
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
