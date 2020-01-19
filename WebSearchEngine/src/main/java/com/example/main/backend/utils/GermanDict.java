package com.example.main.backend.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class GermanDict {
	
	public static HashMap<String, List<String>> loadGermanDictionaryFromFile() throws IOException {
		
		BufferedReader germanDict = new BufferedReader( new FileReader( Utils.createTempFileFromInputStream("openthesaurus.txt")));
		HashMap<String, List<String>> dictMap = new HashMap<String, List<String>>();
		String line;
		while( null != (line = germanDict.readLine())) {
			
			String[] tokens = line.split(";");
			String baseTerm = null;
			for(int i = 0; i < tokens.length; i++) {
				String term = cleanGermanDictWord(tokens[i]).trim().toLowerCase();
				if(i == 0) {
					baseTerm = term;
					dictMap.put(baseTerm, new ArrayList<String>());
				}
				else 
					dictMap.get(baseTerm).add(term);
			}
		}
		germanDict.close();
		return dictMap;
	}
	
	public static String cleanGermanDictWord(String word) {
		//eliminate parantheses
		return word.replaceAll("\\s*\\([^\\)]*\\)\\s*", " ");
	}

	public static boolean isGermanDictionaryLoaded(Connection con) throws SQLException {
		
		PreparedStatement stmt = con.prepareStatement("SELECT FROM germanDict");
		stmt.execute();
		ResultSet res = stmt.getResultSet();
		boolean isLoaded = res.next();
		stmt.close();
		return isLoaded;
	}
	
	public static void saveGermanDictToDB(Connection con) throws SQLException, IOException {
		
		if(!isGermanDictionaryLoaded(con)) {
			
			Map<String, List<String>> dict = loadGermanDictionaryFromFile();
			PreparedStatement stmt = con.prepareStatement("INSERT INTO germanDict(word, syn) VALUES (?, ?)");
	
			for (Entry<String, List<String>> pair : dict.entrySet()) {
				String baseTerm = pair.getKey();
				for(String syn : pair.getValue()) {
					stmt.setString(1, baseTerm);
					stmt.setString(2, syn);
					stmt.addBatch();
				}
			}
			stmt.executeBatch();
			stmt.close();
		}
	}
	
	
	public static Set<String> getSynonymsForGermanTerm(String term, Connection con) throws SQLException{
		
		term = term.toLowerCase();
		Set<String> relTerms = new HashSet<String>();
		String query = 
				"SELECT * FROM ("
				+ "SELECT syn FROM germanDict WHERE word = ?"
				+ ") t1 UNION ("
				+ "SELECT word FROM germanDict WHERE syn = ?"
				+ ") t2";
		PreparedStatement stmt = con.prepareStatement(query);
		stmt.setString(1, term);
		stmt.setString(2, term);
		stmt.execute();
		ResultSet result = stmt.getResultSet();
		while(result.next()) {
			relTerms.add(result.getString(1));
		}
		stmt.close();
		result.close();
		return relTerms;
	}
}
