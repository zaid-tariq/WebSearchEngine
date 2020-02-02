package com.example.main.backend;

import java.io.IOException;
import java.sql.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.main.backend.utils.GermanDict;

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
			createShinglesTable(connection);
			createJaccardValuesTable(connection);
			createMinhashTable(connection);
			createMinhashJaccardValuesTable(connection);
			createUpdateScoresunction(connection);
			createFunctionConjunctive_search(connection);
			createFunctionDisjunctive_search(connection);
			createFunctionDisjunctiveImage_search(connection);
			create_computeJaccardValues_function(connection);
			create_get_similar_documents_function(connection);
			create_makeShingles_function(connection);
			create_minhash_jaccard_function(connection);
			create_minhash_function(connection);
			create_shingle_hashing_function(connection);
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
			createGermanDictTable(connection);
			GermanDict.saveGermanDictToDB(connection);
			create_metasearch_config_table(connection);
			create_metasearch_cori_table(connection);
			create_metasearch_collection_stats_table(connection);
			create_updateCoriStats_function(connection);
			create_getRelevantSearchEngines_function(connection);

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
	
	
	private void createGermanDictTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS germanDict(word TEXT, syn TEXT)");
		statement.execute();
		statement.close();
	}
	
	private void createMinhashTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS minhash_table (docid INT NOT NULL, minhash INT NOT NULL)");
		statement.execute();
		statement.close();
	}
	
	private void createShinglesTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS shingles_table (docid INT NOT NULL, shingle TEXT NOT NULL, PRIMARY KEY (docid, shingle))");
		statement.execute();
		statement.close();
	}
	
	private void createJaccardValuesTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS jaccard_values (d1 int NOT NULL, d2 int NOT NULL, jaccardVal real, PRIMARY KEY(d1,d2))");
		statement.execute();
		statement.close();
	}
	
	private void createMinhashJaccardValuesTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS jaccard_values_minhash (d1 int NOT NULL, d2 int NOT NULL, jaccardVal real, PRIMARY KEY(d1,d2))");
		statement.execute();
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
				"CREATE INDEX IF NOT EXISTS term_index ON features (term);"+ 
				"CREATE INDEX IF NOT EXISTS link_index ON links (from_docid, to_docid);" ;
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void createGetDocFrequenciesFunction(Connection con) throws SQLException {
		String query = 
				"CREATE OR REPLACE FUNCTION get_doc_frequencies(" + "    search_terms text[]" + ")"
				+ "RETURNS TABLE(term text, df int) " + "LANGUAGE 'plpgsql' " 
				+ "AS $$ " + "BEGIN "
				+ "DROP TABLE IF EXISTS search_terms_table; "
				+ "CREATE TEMP TABLE search_terms_table(term text);		"
				+ "INSERT INTO search_terms_table SELECT unnest(search_terms); " 
				+ "RETURN QUERY			"
				+ "		SELECT DISTINCT st.term, f.df			"
				+ "		from features f, search_terms_table st		" 
				+ "		WHERE f.term = st.term;" 
				+ "	END;  $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void createUpdateScoresunction(Connection con) throws SQLException {
		String query = 
		"CREATE OR REPLACE PROCEDURE update_scores(k real, b real)   " + 
		"    LANGUAGE 'plpgsql'   " + 
		"    AS $$   "+ 
		"	 DECLARE avg_num_of_terms real;" +
		"    BEGIN   " + 
		"    SELECT (SELECT d.avg_num_of_terms FROM doc_stats_table d) INTO avg_num_of_terms;"+
		"	 UPDATE features as f   " + 
		"    SET score_tfidf = f.idf_tfidf * (1.0 + LOG(f.term_frequency)) ,   " + 
		"        score_okapi = f.idf_okapi * (f.term_frequency * ($1 + 1) / ( f.term_frequency *($1 * (1-$2+($2 * d.num_of_terms/avg_num_of_terms)))))" + 
		"    FROM features f2, documents d" + 
		"    WHERE f.term = f2.term " + 
		"    AND f.docid = f2.docid " + 
		"    AND f.docid=d.docid;" + 
		"    END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void createUpdateIdfScoresunction(Connection con) throws SQLException {
		String query = 
		"CREATE OR REPLACE PROCEDURE update_idf_scores_function()   " + 
		"    LANGUAGE 'plpgsql'   " + 
		"    AS $$   "+ 
		"	 DECLARE total_docs int;" + 
		"    BEGIN   " + 
		"	 SELECT (SELECT d.total_docs FROM doc_stats_table d) INTO total_docs;	"+
		"    UPDATE features" + 
		"    SET idf_tfidf = LOG(1.0 * total_docs / df), " + 
		"        idf_okapi = LOG((total_docs - df + 0.5)/(df + 0.5));"+
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
		"    TRUNCATE TABLE doc_stats_table;"+
		"    WITH   " + 
		"      docs_stats AS( " + 
		"          SELECT COUNT(docid) num_docs, AVG(num_of_terms) avg_num_terms  " + 
		"          FROM documents  " + 
		"      )" +
	    "    INSERT INTO doc_stats_table (SELECT * FROM docs_stats);" +
		"    END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	
	private void create_get_similar_documents_function(Connection con) throws SQLException {
		
		String query = 
			"CREATE OR REPLACE FUNCTION get_similar_documents(docid int, thresh real)" + 
			"RETURNS TABLE(docid2 int, jaccardVal real)" + 
			"LANGUAGE 'plpgsql'" + 
			"AS $$ " + 
			"BEGIN" + 
			"  RETURN QUERY" + 
			"    SELECT jdp.d2, jdp.jaccardVal" + 
			"    FROM jaccard_values jdp" + 
			"    WHERE jdp.d1 = docid AND jdp.jaccardVal >= thresh;" + 
			"END $$";
		
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	
	private void create_makeShingles_function(Connection con) throws SQLException {
		
		String query = 
			"CREATE OR REPLACE PROCEDURE makeShingles(K int)   " + 
			"    LANGUAGE 'plpgsql'   " + 
			"    AS $$   " + 
			"    DECLARE " + 
			"      t_row documents%rowtype;" + 
			"      content text;" + 
			"      doc_terms text[];" + 
			"      term text;" + 
			"      shingle text;" + 
			"      i int;" + 
			"      j int;" + 
			"" + 
			"    BEGIN   " + 
			"      TRUNCATE TABLE shingles_table;" + 
			"      FOR t_row IN SELECT * FROM documents WHERE num_of_terms > 0" + 
			"      LOOP" + 
			"          content := t_row.content;" + 
			"          doc_terms := regexp_split_to_array(content, E'\\\\s+');" + 
			"" + 
			"          FOR i in 1 .. ( array_length(doc_terms,1) - K )" + 
			"          LOOP" + 
			"            shingle := doc_terms[i];" + 
			"" + 
			"            FOR j in 1 .. K" + 
			"            LOOP" + 
			"              term := doc_terms[i+j];" + 
			"              shingle := shingle || ' ' || term;" + 
			"            END LOOP;" + 
			"" + 
			"            INSERT INTO shingles_table" + 
			"            VALUES (t_row.docid, shingle)" + 
			"            ON CONFLICT DO NOTHING;" + 
			"" + 
			"          END LOOP;" + 
			"      END LOOP;" + 
			"    END; $$;";
		
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void create_shingle_hashing_function(Connection con) throws SQLException{
		
		String query = 
				"CREATE OR REPLACE FUNCTION hash_shingle_to_int(shingle text) RETURNS bigint AS $$" + 
				"    DECLARE" + 
				"        result  bigint;" + 
				"    BEGIN" + 
				"        EXECUTE 'SELECT x' || quote_literal(substr(md5(shingle), 1,7)) || '::int' INTO result;" + 
				"        RETURN result;" + 
				"    END;" + 
				"    $$ LANGUAGE plpgsql;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
		
	}
	
	private void create_minhash_function(Connection con) throws SQLException{
		
		String query = 
				"CREATE OR REPLACE PROCEDURE do_minhash_table()   " + 
				"    LANGUAGE 'plpgsql'   " + 
				"    AS $$   " + 
				"    BEGIN" + 
				"      TRUNCATE minhash_table;" + 
				"      INSERT INTO minhash_table" + 
				"        SELECT docid, hash_shingle_to_int(shingle) minhash" + 
				"        FROM shingles_table;" + 
				"    END; $$";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
		
	}


	private void create_minhash_jaccard_function(Connection con) throws SQLException{
		
		String query = 
				"CREATE OR REPLACE PROCEDURE calculate_jaccard_minhash(N int)" + 
				"    LANGUAGE 'plpgsql'   " + 
				"    AS $$   " + 
				"    BEGIN" + 
				"	 TRUNCATE TABLE jaccard_values_minhash;"+
				"    WITH n_minhash_table AS (" + 
				"        SELECT  t2.docid, array_agg(t2.minhash) hashes" + 
				"        FROM (" + 
				"          SELECT DISTINCT docid" + 
				"          FROM minhash_table" + 
				"        ) d1 JOIN LATERAL (" + 
				"          SELECT docid, minhash" + 
				"          FROM minhash_table m2" + 
				"          WHERE m2.docid = d1.docid" + 
				"          ORDER BY minhash asc" + 
				"          LIMIT N" + 
				"        ) t2 ON true" + 
				"        GROUP BY t2.docid"	+ 
				"		 LIMIT 10000" + 
				"      )" + 
				"    INSERT INTO jaccard_values_minhash"+
				"	 SELECT st1.docid d1, st2.docid d2,  (" + 
				"                    (" + 
				"                      SELECT COUNT(*)" + 
				"                      FROM (" + 
				"                        SELECT unnest(st1.hashes)" + 
				"                        INTERSECT " + 
				"                        SELECT unnest(st2.hashes)" + 
				"                      ) a1" + 
				"                    ) * 1.0 / (" + 
				"                       SELECT COUNT(*)" + 
				"                       FROM (" + 
				"                        SELECT unnest(st1.hashes) " + 
				"                        UNION " + 
				"                        SELECT unnest(st2.hashes)" + 
				"                      ) a2" + 
				"                    )" + 
				"              ) jaccardVal" + 
				"      FROM n_minhash_table st1, n_minhash_table st2;" + 
				"  END; $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
		
	}


	private void create_computeJaccardValues_function(Connection con) throws SQLException {
		
		String query = 
			"CREATE OR REPLACE PROCEDURE computeJaccardValues()  " + 
			"    LANGUAGE 'plpgsql'   " + 
			"    AS $$   " + 
			"    BEGIN" + 
			"	   TRUNCATE TABLE jaccard_values;"+
			"      WITH shingles_table_temp AS (" + 
			"          SELECT docid, array_agg(shingle) shingles" + 
			"          FROM shingles_table" + 
			"          GROUP BY docid" + 
			"		   LIMIT 10000" +  
			"        )" + 
			"      INSERT INTO jaccard_values" + 
			"      SELECT st1.docid d1, st2.docid d2,  (" + 
			"                    (" + 
			"                      SELECT COUNT(*)" + 
			"                      FROM (" + 
			"                        SELECT unnest(st1.shingles)" + 
			"                        INTERSECT " + 
			"                        SELECT unnest(st2.shingles)" + 
			"                      ) a1" + 
			"                    ) * 1.0 / (" + 
			"                       SELECT COUNT(*)" + 
			"                       FROM (" + 
			"                        SELECT unnest(st1.shingles) " + 
			"                        UNION " + 
			"                        SELECT unnest(st2.shingles)" + 
			"                      ) a2" + 
			"                    )" + 
			"              ) jaccardVal" + 
			"      FROM shingles_table_temp st1, shingles_table_temp st2;" + 
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
	
	
	private void create_metasearch_config_table(Connection con) throws SQLException{
		
		String query = 
				"CREATE TABLE IF NOT EXISTS metasearch_config(engine_url TEXT PRIMARY KEY, enabled boolean)";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void create_metasearch_cori_table(Connection con) throws SQLException{
		
		String query = 
				"CREATE TABLE IF NOT EXISTS metasearch_cori(engine_url TEXT, term TEXT, T_score real, I_score real, df int, PRIMARY KEY(engine_url, term)";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
	private void create_metasearch_collection_stats_table(Connection con) throws SQLException{
		
		String query = 
				"CREATE TABLE IF NOT EXISTS metasearch_collection_stats(engine_url TEXT PRIMARY KEY, cw int)"; //can also be used to get avg_cw
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void create_getRelevantSearchEngines_function(Connection con) throws SQLException{
		
		String query = 
				"CREATE OR REPLACE FUNCTION getRelevantSearchEngines(search_query text) " + 
				"RETURNS TABLE(engine_url text, term text, T_score real, I_score real) " + 
				"LANGUAGE 'plpgsql' " + 
				"AS " + 
				"$$ " + 
				"BEGIN" + 
				"	CREATE TEMP TABLE IF NOT EXISTS search_terms(term text);" + 
				"	TRUNCATE search_terms;" + 
				"	INSERT INTO search_terms (SELECT unnest(regexp_split_to_array(search_query, E'\\\\s+')));" + 
				"	RETURN QUERY " + 
				"		WITH enabled_engines AS(" + 
				"				SELECT * FROM metasearch_config WHERE enabled = true" + 
				"			)" + 
				"		SELECT ee2.engine_url, t.term, t.T_score, t.I_score" + 
				"		FROM enabled_engines ee2 LEFT JOIN(" + 
				"			SELECT mc.engine_url, mc.term, mc.T_score, mc.I_score" + 
				"			FROM enabled_engines ee JOIN " + 
				"			metasearch_cori mc ON (ee.engine_url=mc.engine_url)" + 
				"			JOIN search_terms st ON (mc.term=st.term)" + 
				"		) t" + 
				"		ON (ee2.engine_url=t.engine_url)" + 
				"		ORDER BY t.engine_url;" + 
				"		" + 
				"END $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
		
	}
	
	private void create_updateCoriStats_function(Connection con) throws SQLException{
		
		String query = 
				"CREATE OR REPLACE PROCEDURE updateCoriStats() " + 
				"LANGUAGE 'plpgsql' " + 
				"AS $$ " + 
				"DECLARE  " + 
				"	avg_cw real; " + 
				"	C int; " + 
				"BEGIN " + 
				"		SELECT AVG(cw), COUNT(DISTINCT engine_url)  " + 
				"		FROM metasearch_collection_stats  " + 
				"		INTO avg_cw, C; " + 
				"		 " + 
				"		UPDATE metasearch_cori " + 
				"		SET  " + 
				"			t_score = df / (df+50+(150*cw/avg_cw)), " + 
				"			i_score = LOG((C+0.5)/cf)/LOG(C+1.0) " + 
				"		FROM metasearch_cori, ( " + 
				"			SELECT term, COUNT(DISTINCT engine_url )as cf " + 
				"			FROM metasearch_cori mc " + 
				"			WHERE df > 0 " + 
				"			GROUP BY term " + 
				"		) term_stats " + 
				"		WHERE  metasearch_cori.term = term_stats.term; " + 
				" " + 
				"END $$;"; 
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
	
}