package com.example.main;

import java.net.URISyntaxException;
import java.sql.SQLException;
import com.example.main.backend.DatabaseCreator;

public class CLIIndexing {

	public static void main(String[] args) throws SQLException, URISyntaxException {

		DatabaseCreator db = new DatabaseCreator();
		java.sql.Connection connection = db.getConnection();

		try {

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
