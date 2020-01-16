package com.example.main.backend;

import java.sql.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCreator {

	@Autowired
	DBHandler db;


	public void create() {
		Connection connection = null;
		try {
			connection = db.getConnection();

			
			// CREATE TABLE IF NOT EXISTS s by DDL statements
			createExtensionLevenshtein(connection);
			createDocumentsTable(connection);
			createFeatureTable(connection);
			createImageFeatureTable(connection);
			createViewsOnFeaturesTable(connection);
			createLinksTable(connection);
			createDocumnetStatsTable(connection);
			createUpdateScoresunction(connection);
			createFunctionConjunctive_search(connection);
			createFunctionDisjunctive_search(connection);
			createFunctionDisjunctiveImage_search(connection);
			createIndices(connection);
			create_function_get_related_terms_to_less_frequent_terms(connection);
			create_alternate_query_scorer_function(connection);
			createCollectionVocabularyTable(connection);
			createCrawlerStateTable(connection);
			createCrawlerQueueTable(connection);
			createCrawlerVisitedPagesTable(connection);
			createUpdateDocStatsFunction(connection);
			createUpdateDocFrequenceFunction(connection);
			createUpdateIdfScoresunction(connection);
			createUpdateScoresunction(connection);
			createGetDocFrequenciesFunction(connection);

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
	
	
	private void createExtensionLevenshtein(Connection connection) throws SQLException {
		String query = "CREATE EXTENSION IF NOT EXISTS fuzzystrmatch";
		Statement statement = connection.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void createFunctionConjunctive_search(Connection connection) throws SQLException {
		String query = 
		"CREATE OR REPLACE FUNCTION get_docs_for_conjunctive_search(search_terms text[], lang TEXT[])"
		+ "    	RETURNS TABLE(docurl text, term text, tfidf real, okapi real, combined real)" 
		+ "    	LANGUAGE 'plpgsql'"
		+ "		AS $$"
				+ " BEGIN" + 
				" 		 CREATE TEMP TABLE search_terms_table(term text);				" + 
				" 		 INSERT INTO search_terms_table SELECT unnest(search_terms); 		" + 
				" 		 RETURN QUERY" + 
				" 		 				WITH" + 
				"		 					filtered_docs AS(						" + 
				"		 						SELECT f1.docid						" + 
				"		 						FROM features f1						" + 
				"		 						GROUP BY f1.docid						" + 
				"		 						HAVING NOT EXISTS(						" + 
				"		 							SELECT * FROM search_terms_table 						" + 
				"		 							EXCEPT SELECT unnest(array_agg(f1.term))				" + 
				"		 							)					" + 
				"		 						)				" + 
				" 		 				SELECT d.url, f.term, f.score_tfidf, f.score_okapi, f.score_combined" + 
				" 		 				from features f, filtered_docs fd, documents d				" + 
				" 		 				WHERE 	f.docid = fd.docid 						" + 
				" 		 				AND f.docid = d.docid"
				+ "						AND d.language = ANY(lang) "+
				" 		 				ORDER BY d.docid;	" + 
				" 		 END; $$;";
		Statement statement = connection.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	

	private void createFunctionDisjunctive_search(Connection connection) throws SQLException {
		String query = 
				"CREATE OR REPLACE FUNCTION get_docs_for_disjunctive_search(search_terms text[], required_terms text[], lang TEXT[]) " + 
				" RETURNS TABLE(docurl text, term text, tfidf real, okapi real) " + 
				" LANGUAGE 'plpgsql' " + 
				"AS $$ " + 
				"BEGIN    " + 
				"" + 
				"	CREATE TEMP TABLE required_terms_table AS" + 
				"	  SELECT unnest(required_terms) term;" + 
				"	  " + 
				"	 CREATE TEMP TABLE search_terms_table AS" + 
				"	  SELECT unnest(search_terms) term;" + 
				"" + 
				"	CREATE TEMP TABLE expanded_terms AS" + 
				"		SELECT t2.elem term, elem2 syn" + 
				"		FROM" + 
				"		(" + 
				"			select *" + 
				"			from search_terms_table st," + 
				"			unnest(string_to_array(split_part(st.term, '=', 1), ':')) elem" + 
				"		  )  t2 " + 
				"		  LEFT JOIN (" + 
				"			select *" + 
				"			from search_terms_table st," + 
				"			unnest(string_to_array(split_part(st.term, '=', 1), ':')) elem," + 
				"			unnest(string_to_array(split_part(st.term, '=', 2), ':')) elem2" + 
				"		  )  t3" + 
				"		  ON" + 
				"		  t2.elem = t3.elem;" + 
				"  CREATE TEMP TABLE combined_expanded_terms (term text);" + 
				"  INSERT INTO combined_expanded_terms" + 
				"    SELECT * " + 
				"    FROM  (" + 
				"        SELECT syn FROM expanded_terms e1" + 
				"        UNION SELECT e1.term FROM expanded_terms e1" + 
				"        UNION SELECT e2.term FROM required_terms_table e2" + 
				"	) s;" + 
				"    " + 
				"" + 
				"  RETURN QUERY" + 
				" 	SELECT d.url, f3.term, f3.score_tfidf, f3.score_okapi  " + 
				"	FROM(" + 
				"		SELECT f.docid" + 
				"		FROM features f  " + 
				"		GROUP BY f.docid   " + 
				"		HAVING   " + 
				"		   NOT EXISTS(           " + 
				"		   SELECT * FROM required_terms_table            " + 
				"		   EXCEPT SELECT unnest(array_agg(f.term) )          " + 
				"		   )  " + 
				"		 AND EXISTS(           " + 
				"			SELECT * FROM combined_expanded_terms             " + 
				"			INTERSECT   " + 
				"			SELECT unnest(array_agg(f.term) )  " + 
				"		 )" + 
				"		) f2 JOIN documents d ON f2.docid = d.docid  " + 
				"		JOIN features f3 ON f2.docid=f3.docid" + 
				"		JOIN combined_expanded_terms cet ON f3.term=cet.term		" + 
				"		WHERE d.language = ANY(lang);  " + 
				"" + 
				"  DROP TABLE expanded_terms;" + 
				"  DROP TABLE combined_expanded_terms;" + 
				"  DROP TABLE required_terms_table;" + 
				"  DROP TABLE search_terms_table;" + 
				"" + 
				"  RETURN; " + 
				"END; $$;";
				Statement statement = connection.createStatement();
				statement.execute(query);
				statement.close();
	}
	
	public void createFunctionDisjunctiveImage_search(Connection connection) throws SQLException {
		String query = 
				"CREATE OR REPLACE FUNCTION get_images_for_disjunctive_search(search_terms text[], required_terms text[], lang TEXT[])"
				+ "    	RETURNS TABLE(imageurl text, docurl text, term text, exponential double precision)" 
				+ "    	LANGUAGE 'plpgsql'"
				+ "		AS $$" +
						" BEGIN" + 
						"	CREATE TEMP TABLE search_terms_table(term text);		" + 
						"	INSERT INTO search_terms_table SELECT unnest(search_terms); " + 
						"	CREATE TEMP TABLE required_terms_table(term text);		" + 
						"	INSERT INTO required_terms_table SELECT unnest(required_terms); " + 
						"	RETURN QUERY" + 
						"			WITH" + 
						" 			filtered_docs_keywords AS(" + 
						" 							SELECT f1.docid				" + 
						" 							FROM imagefeatures f1				" + 
						" 							GROUP BY f1.docid 				" + 
						" 							HAVING " + 
						" 								NOT EXISTS(					" + 
						"	 								SELECT * FROM required_terms_table 					" + 
						"	 								EXCEPT SELECT unnest(array_agg(f1.term))				" + 
						"	 								)" + 
						"	 							AND EXISTS(					" + 
						"		 							(SELECT * FROM search_terms_table UNION SELECT * FROM required_terms_table) 						" + 
						"		 							INTERSECT " + 
						"		 							SELECT unnest(array_agg(f1.term))" + 
						"		 						)			" + 
						" 							)					" + 
						" 						SELECT f.imageurl, d.url, f.term, f.score_exponential" + 
						" 		 				from imagefeatures f, filtered_docs_keywords fd, documents d				" + 
						" 		 				WHERE 	f.docid = fd.docid 						" + 
						" 		 				AND f.docid = d.docid"
						+ "						AND d.language = ANY(lang)			" + 
						" 		 				ORDER BY d.docid;	" + 
						" END; $$;";
				Statement statement = connection.createStatement();
				statement.execute(query);
				statement.close();
	}

	private void createFeatureTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS features (docid INT REFERENCES documents(docid), term TEXT, term_frequency INT, score_tfidf real, score_okapi real, score_combined real, idf_tfidf real, idf_okapi real, df int )");
		statement.execute();
		statement.close();
	}
	
	private void createImageFeatureTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS imagefeatures(imageurl TEXT NOT NULL, docid INT REFERENCES documents(docid), term TEXT, ndist INT, score_exponential DOUBLE PRECISION)");
		statement.execute();
		statement.close();
	}

	private void createDocumentsTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS documents (docid SERIAL PRIMARY KEY, url TEXT NOT NULL UNIQUE , crawled_on_date DATE, language TEXT, page_rank DOUBLE PRECISION, num_of_terms int, content TEXT)");
		statement.execute();
		statement.close();
	}

	private void createLinksTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS links (from_docid INT REFERENCES documents(docid), to_docid INT REFERENCES documents(docid))");
		statement.execute();
		statement.close();
	}

	private void createCrawlerQueueTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS crawlerQueue (id SERIAL PRIMARY KEY, url TEXT NOT NULL, current_depth INT NOT NULL)");
		statement.execute();
		statement.close();
	}

	private void createCrawlerVisitedPagesTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS crawlerVisitedPages (url TEXT NOT NULL, last_visited DATE)");
		statement.execute();
		statement.close();
	}

	private void createCrawlerStateTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS crawlerState (maximum_depth INT NOT NULL, maximum_docs INT NOT NULL, crawled_docs INT NOT NULL, leave_domain BOOLEAN, parallelism INT NOT NULL, run BOOLEAN DEFAULT TRUE, domains TEXT[])");
		statement.execute();
		statement.close();
	}
	
	private void createCollectionVocabularyTable(Connection con) throws SQLException {
		//not using this table right now, but can use it for optimizations later
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS collection_vocab_table (term text NOT NULL, frequency INT NOT NULL, doc_freq INT NOT NULL)");
		statement.execute();
		statement.close();
	}
	
	private void createDocumnetStatsTable(Connection con) throws SQLException {
		
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS doc_stats_table (total_docs INT, avg_num_of_terms INT)");
		statement.execute();
		statement.close();
	}
	
	private void createIndices(Connection con) throws SQLException {
	
		String query = 
				"CREATE INDEX IF NOT EXISTS docid_index ON features (docid);" + 
				"CREATE INDEX IF NOT EXISTS term_index ON features (term);" ;
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}


	private void createGetDocFrequenciesFunction(Connection con) throws SQLException {
		String query = "CREATE OR REPLACE FUNCTION get_doc_frequencies(" + "    search_terms text[]" + ")"
				+ "RETURNS TABLE(term text, df bigint) " + "LANGUAGE 'plpgsql' " + "AS $$ " + "BEGIN "
				+ "DROP TABLE IF EXISTS search_terms_table; "
				+ "CREATE TEMP TABLE search_terms_table(term text);		"
				+ "INSERT INTO search_terms_table SELECT unnest(search_terms); " + "RETURN QUERY			"
				+ "		SELECT st.term, COUNT(DISTINCT f.docid) as df			"
				+ "		from features f, search_terms_table st		" + "		WHERE f.term = st.term"
				+ "		GROUP BY st.term;" + "	END;  $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void createUpdateScoresunction(Connection con) throws SQLException {
		String query = 
		"" + 
		"CREATE OR REPLACE PROCEDURE update_scores(k real, b real)   " + 
		"    LANGUAGE 'plpgsql'   " + 
		"    AS $$   " + 
		"    BEGIN   " + 
		"    " + 
		"    UPDATE features f   " + 
		"    SET score_tfidf = f.idf_tfidf * (1.0 + LOG(f.term_frequency)) ,   " + 
		"        score_okapi = f.idf_okapi * ( f.term_frequency * ($1 + 1) / ( f.term_frequency *($1 * (1-$2+($2 * d.num_of_terms/doc_stats_table.avg_num_of_terms)))))  " + 
		"    FROM features f2, doc_stats_table, documents d" + 
		"    WHERE f.term = f2.term " + 
		"    AND f.docid = f2.docid " + 
		"    AND f.docid=d.docid;" + 
		"     " + 
		"    END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void createUpdateIdfScoresunction(Connection con) throws SQLException {
		String query = 
		"CREATE OR REPLACE PROCEDURE update_idf_scores_function()   " + 
		"    LANGUAGE 'plpgsql'   " + 
		"    AS $$   " + 
		"    BEGIN   " + 
		" " + 
		"      UPDATE features f1" + 
		"      SET idf_tfidf = LOG(1.0 * doc_stats_table.total_docs / f2.df), " + 
		"          idf_okapi = LOG((doc_stats_table.total_docs - f2.df + 0.5)/(f2.df + 0.5)) " + 
		"      FROM features f2,doc_stats_table" + 
		"      WHERE f1.term = f2.term AND f1.docid = f2.docid;" + 
		"" + 
		"    END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	
	private void createUpdateDocFrequenceFunction(Connection con) throws SQLException {
		String query = 
		"CREATE OR REPLACE PROCEDURE update_df_table()   " + 
		"    LANGUAGE 'plpgsql'   " + 
		"    AS $$   " + 
		"    BEGIN   " + 
		"      UPDATE features" + 
		"      SET df = df_tbl.doc_count_of_term" + 
		"      FROM (" + 
		"	  	  SELECT f2.term, COUNT(DISTINCT f2.docid) AS doc_count_of_term  " + 
		"		  FROM features f2  " + 
		"		  GROUP BY f2.term  " + 
		"	  ) df_tbl" + 
		"      WHERE features.term = df_tbl.term;" + 
		"    END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	
	private void createUpdateDocStatsFunction(Connection con) throws SQLException {
		String query = 
		"CREATE OR REPLACE PROCEDURE update_doc_stats_table()   " + 
		"    LANGUAGE 'plpgsql'   " + 
		"    AS $$   " + 
		"    BEGIN   " + 
		"    WITH   " + 
		"      docs_stats AS( " + 
		"          SELECT COUNT(docid) num_docs, AVG(num_of_terms) avg_num_terms  " + 
		"          FROM documents  " + 
		"      )" + 
		"      UPDATE doc_stats_table" + 
		"      SET total_docs = ds.num_docs," + 
		"          avg_num_of_terms = ds.avg_num_terms" + 
		"      FROM docs_stats ds;" + 
		"    END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	
	private void createViewsOnFeaturesTable(Connection con) throws SQLException {
		
		String query = "CREATE OR REPLACE VIEW features_tfidf AS" + 
				"	SELECT docid, term, score_tfidf AS score" + 
				"	FROM features; " + 
				"" + 
				"CREATE OR REPLACE VIEW features_bm25 AS" + 
				"	SELECT docid, term, score_okapi AS score" + 
				"	FROM features;"+ 
				"" + 
				"CREATE OR REPLACE VIEW features_combined AS" + 
				"	SELECT docid, term, score_combined AS score" + 
				"	FROM features;";
		
		
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void create_function_get_related_terms_to_less_frequent_terms(Connection con) throws SQLException{
		String query = 
			"CREATE OR REPLACE FUNCTION get_related_terms_to_non_existant_or_rare_terms(search_terms text[], dist_thresh int, rarity_thresh int)" + 
				"RETURNS TABLE(query_term text, related_term text, distance int)" + 
				"LANGUAGE 'plpgsql' " + 
				"AS " + 
				"$$ " + 
				"BEGIN " + 
				"	CREATE TEMP TABLE IF NOT EXISTS search_terms_table(term text) ON COMMIT DROP;" + 
				"	INSERT INTO search_terms_table SELECT unnest(search_terms);" + 
				"	RETURN QUERY " + 
				"		WITH " + 
				"			query_terms AS (" + 
				"				SELECT DISTINCT f.term" + 
				"				FROM features f, search_terms_table st" + 
				"				WHERE f.term = st.term AND f.df <= rarity_thresh" + 
				"				UNION" + 
				"				SELECT st.term" + 
				"				FROM search_terms_table st" + 
				"				)," + 
				"			collection_terms AS (" + 
				"				SELECT f1.term FROM features f1 WHERE length(f1.term) < 255 " + 
				"				EXCEPT " + 
				"				SELECT qt.term FROM query_terms qt" + 
				"				)" + 
				"	  SELECT qt.term, ct.term, levenshtein(qt.term, ct.term) AS dist" + 
				"	  FROM query_terms qt, collection_terms ct " + 
				"	  WHERE levenshtein(qt.term, ct.term) <= dist_thresh;" + 
				"END; " + 
				"$$;";
		
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void create_alternate_query_scorer_function(Connection con) throws SQLException {
		String query = 
				"CREATE OR REPLACE FUNCTION get_df_scores_of_term_pairs(list_of_term_pairs text[]) " + 
				"RETURNS TABLE(score int) " + 
				"LANGUAGE 'plpgsql' " + 
				"AS " + 
				"$$ " + 
				"BEGIN" + 
				"  CREATE TEMP TABLE term_pairs_table(term1 text, term2 text);" + 
				"  INSERT INTO term_pairs_table" + 
				"              SELECT split_part(term_pair, ':', 1) as term1, split_part(term_pair, ':', 2) as term2" + 
				"              FROM (SELECT unnest(list_of_term_pairs) as term_pair) tab;" + 
				"  RETURN QUERY" + 
				"    WITH" + 
				"      inverted_index_doc_to_term AS (" + 
				"          SELECT docid, array_agg(distinct term) as cont_docs" + 
				"          FROM features" + 
				"          GROUP BY docid" + 
				"      )" + 
				"      SELECT  COUNT(docid)" + 
				"      FROM inverted_index_doc_to_term iitd, term_pairs_table pairs" + 
				"      WHERE pairs.term1 IN (SELECT unnest(iitd.cont_docs) as term1)" + 
				"            AND pairs.term2 IN (SELECT unnest(iitd.cont_docs) as term2); " + 
				"END;$$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	
}