package com.example.main.backend.utils;

import java.sql.SQLException;
import org.springframework.stereotype.Service;

import com.example.main.backend.DBHandler;

@Service
public class DocumentSimilarity {
	
	public static void updateActualJaccardValuesTable(DBHandler db) throws SQLException {
		db.callJaccardShinglesFunction();
	}
	
	public static void updateApproxJaccardValuesTable(DBHandler db) throws SQLException {
		db.callJaccardMinhashFunction(32);
	}
	
	
	public static void updateShinglesTable(DBHandler db) throws SQLException {
		db.callMakeShinglesFunction(4);
	}
	
	public static void updateMinhashTalble(DBHandler db) throws SQLException {
		db.callDoMinhashFunction();
	}
	
	public static void run(DBHandler db) throws SQLException {
		updateShinglesTable(db);
		System.out.println("updateShinglesTable done");
		updateMinhashTalble(db);
		System.out.println("updateMinhashTalble done");
		updateApproxJaccardValuesTable(db);
		System.out.println("updateApproxJaccardValuesTable done");
		updateActualJaccardValuesTable(db);
		System.out.println("updateActualJaccardValuesTable done");
	}
}
