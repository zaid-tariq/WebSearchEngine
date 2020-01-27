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
	
	public static String printErrors(DBHandler db) throws SQLException {
		String res = "";
		db.callJaccardMinhashFunction(1);
		float err = db.getAverageJaccardError();
		res += "\n" +("Avg Error for n=1: "+err);
		err = db.getJaccardQuartileError((float) 0.5);
		res += "\n" +("Median Error for n=1: "+err);
		err = db.getJaccardQuartileError((float) 0.25);
		res += "\n" +("First Quartile Error for n=1: "+err);
		err = db.getJaccardQuartileError((float) 0.75);
		res += "\n" +("Third Quartile Error for n=1: "+err);
		
		db.callJaccardMinhashFunction(4);
		 err = db.getAverageJaccardError();
		res += "\n" +("Avg Error for n=4: "+err);
		err = db.getJaccardQuartileError((float) 0.5);
		res += "\n" +("Median Error for n=4: "+err);
		err = db.getJaccardQuartileError((float) 0.25);
		res += "\n" +("First Quartile Error for n=4: "+err);
		err = db.getJaccardQuartileError((float) 0.75);
		res += "\n" +("Third Quartile Error for n=4: "+err);
		
		db.callJaccardMinhashFunction(16);
		 err = db.getAverageJaccardError();
		res += "\n" +("Avg Error for n=16: "+err);
		err = db.getJaccardQuartileError((float) 0.5);
		res += "\n" +("Median Error for n=16: "+err);
		err = db.getJaccardQuartileError((float) 0.25);
		res += "\n" +("First Quartile Error for n=16: "+err);
		err = db.getJaccardQuartileError((float) 0.75);
		res += "\n" +("Third Quartile Error for n=16: "+err);
		
		db.callJaccardMinhashFunction(32);
		 err = db.getAverageJaccardError();
		res += "\n" +("Avg Error for n=32: "+err);
		err = db.getJaccardQuartileError((float) 0.5);
		res += "\n" +("Median Error for n=32: "+err);
		err = db.getJaccardQuartileError((float) 0.25);
		res += "\n" +("First Quartile Error for n=32: "+err);
		err = db.getJaccardQuartileError((float) 0.75);
		res += "\n" +("Third Quartile Error for n=32: "+err);
		
		return res;
		
	}
	
	
	public static String updateJaccardTables(DBHandler db) throws SQLException {
		String res = "";
		updateShinglesTable(db);
		res += "\n" +("updateShinglesTable done");
		updateMinhashTalble(db);
		res += "\n" +("updateMinhashTalble done");
		updateApproxJaccardValuesTable(db);
		res += "\n" +("updateApproxJaccardValuesTable done");
		updateActualJaccardValuesTable(db);
		res += "\n" +("updateActualJaccardValuesTable done");
		return res;
	}
}
