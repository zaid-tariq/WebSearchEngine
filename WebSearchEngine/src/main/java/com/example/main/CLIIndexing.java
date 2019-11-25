package com.example.main;

import java.net.URISyntaxException;
import java.sql.SQLException;
import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.IndexingThread;

public class CLIIndexing {

	public static void main(String[] args) throws SQLException, URISyntaxException {

		DatabaseCreator db = new DatabaseCreator();
		java.sql.Connection connection = db.getConnection();

		try {
			new IndexingThread(Integer.parseInt(args[0])).start();
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
