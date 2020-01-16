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
			createUpdateScoresunction(connection);
			createFunctionConjunctive_search(connection);
			createFunctionDisjunctive_search(connection);
			createStatsFunction(connection);
			createIndices(connection);
			create_function_get_related_terms_to_less_frequent_terms(connection);
			create_alternate_query_scorer_function(connection);

			createCrawlerStateTable(connection);
			createCrawlerQueueTable(connection);
			createCrawlerVisitedPagesTable(connection);

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
				"CREATE OR REPLACE FUNCTION get_docs_for_disjunctive_search(search_terms text[], required_terms text[], lang TEXT[])"
				+ "    	RETURNS TABLE(docurl text, term text, tfidf real, okapi real, combined real)" 
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
						" 							FROM features f1				" + 
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
						" 						SELECT d.url, f.term, f.score_tfidf, f.score_okapi, f.score_combined" + 
						" 		 				from features f, filtered_docs_keywords fd, documents d				" + 
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
	
	private void createIndices(Connection con) throws SQLException {
	
		String query = 
				"CREATE INDEX IF NOT EXISTS docid_index ON features (docid);" + 
				"CREATE INDEX IF NOT EXISTS term_index ON features (term);" ;
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
/*
	private void createProcedureToTfIdfUpdate(Connection con) throws SQLException {
		String query = "CREATE OR REPLACE PROCEDURE update_tf_idf_scores() " + " LANGUAGE 'sql' "
				+ " AS $procedure$	CREATE TABLE features_temp AS" + "		WITH " + "			doc_freq AS ("
				+ "				SELECT term, COUNT(DISTINCT docid) as doc_count_of_term"
				+ "				FROM features" + "				GROUP BY term" + "			),"
				+ "			total_docs AS (" + "				SELECT COUNT( DISTINCT docid) as num"
				+ "				FROM features" + "			)," + "			idf_scores AS ("
				+ "				SELECT f.term, LOG( 1.0 * total_docs.num / doc_freq.doc_count_of_term) as idf_score"
				+ "				FROM features f, doc_freq, total_docs			"
				+ "				WHERE f.term = doc_freq.term" + "			)"
				+ "		SELECt f.docid, f.term, f.term_frequency, (i.idf_score * (1.0 + LOG(f.term_frequency) )) AS tf_idf"
				+ "		FROM idf_scores i, features f" + "		WHERE i.term = f.term;" + "		"
				+ "	DROP TABLE features;" + "	ALTER TABLE features_temp" + "		RENAME TO features;" + ""
				+ "	$procedure$";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void createFunctionConjunctive_search(Connection con) throws SQLException {
		String query = "		CREATE OR REPLACE FUNCTION conjunctive_search(" + "				IN search_terms text[],"
				+ "				IN top_k integer" + "			)"
				+ "    	RETURNS TABLE(doc_url text, tf_idf_score real)" + "    	LANGUAGE 'plpgsql'"
				+ "		AS $BODY$ BEGIN" + "		CREATE TEMP TABLE search_terms_table(term text);		"
				+ "		INSERT INTO search_terms_table SELECT unnest(search_terms); " + "		RETURN QUERY"
				+ "				WITH " + "					filtered_docs AS(" + "						SELECT f1.docid"
				+ "						FROM features f1" + "						GROUP BY f1.docid"
				+ "						HAVING NOT EXISTS(" + "						SELECt * FROM search_terms_table "
				+ "						EXCEPT SELECT unnest(array_agg(f1.term))" + "				)"
				+ "					)" + "				SELECT d.url, CAST( SUM(tf_idf) AS FLOAT(3)) as tf_idf"
				+ "				from features f, filtered_docs fd, documents d"
				+ "				WHERE 	f.docid = fd.docid " + "						AND f.docid = d.docid"
				+ "				GROUP BY d.url" + "				ORDER BY tf_idf" + "				LIMIT top_k;"
				+ "END; $BODY$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void createFunctionDisjunctive_search(Connection con) throws SQLException {
		String query = "	CREATE OR REPLACE FUNCTION disjunctive_search(" + "    	search_terms text[], "
				+ "		required_terms text[]," + "		top_k integer" +
				// " site text" +
				"		)" + "	RETURNS TABLE(doc_url text, tf_idf_score real)" + "	LANGUAGE plpgsql" + "	AS $$" +

				"BEGIN			" + "CREATE TEMP TABLE search_terms_table(term text);		"
				+ "INSERT INTO search_terms_table SELECT unnest(search_terms); "
				+ "CREATE TEMP TABLE required_terms_table(term text);		"
				+ "INSERT INTO required_terms_table SELECT unnest(required_terms); " + "RETURN QUERY" + "		WITH "
				+ "			filtered_docs_keywords AS(						" + "				SELECT f1.docid"
				+ "				FROM features f1" + "				GROUP BY f1.docid "
				+ "				HAVING NOT EXISTS(" + "					SELECt * FROM required_terms_table "
				+ "					EXCEPT SELECT unnest(array_agg(term))" + "				)" + "			)"
				+ "			" + "		SELECT d.url, CAST(SUM(f.tf_idf) AS FLOAT(2)) as tf_idf				"
				+ "		from features f, filtered_docs_keywords fd, search_terms_table st, documents d		"
				+ "		WHERE	f.docid=fd.docid						" + "				AND f.term = st.term "
				+ "				AND f.docid = d.docid" + "		GROUP BY d.url				"
				+ "		ORDER BY tf_idf				" + "		LIMIT top_k;" + "	END;  $$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}*/

	private void createStatsFunction(Connection con) throws SQLException {
		String query = "CREATE OR REPLACE FUNCTION get_term_frequencies(" + "    search_terms text[]" + ")"
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
		"CREATE OR REPLACE PROCEDURE update_scores(k real, b real) " + 
		"LANGUAGE 'plpgsql' " + 
		"AS $$ " + 
		"BEGIN " + 
		"WITH " + 
		"  doc_freq AS(" + 
		"      SELECT term," + 
		"      COUNT(DISTINCT docid) AS doc_count_of_term" + 
		"      FROM features" + 
		"      GROUP BY term" + 
		"  )," + 
		"    " + 
		"  docs_stats AS(" + 
		"      SELECT COUNT(docid) AS total_docs, AVG(num_of_terms) as avg_num_of_terms" + 
		"      FROM documents" + 
		"  )," + 
		"  idf_scores_tfidf AS(" + 
		"      SELECT f.term," + 
		"          LOG(1.0 * docs_stats.total_docs / doc_freq.doc_count_of_term) AS idf_score" + 
		"      FROM features f," + 
		"          doc_freq," + 
		"          docs_stats" + 
		"      WHERE f.term = doc_freq.term " + 
		"  )," + 
		"  idf_scores_okapi AS(" + 
		"      SELECT f.term," + 
		"              LOG((docs_stats.total_docs - doc_freq.doc_count_of_term + 0.5)/(doc_freq.doc_count_of_term + 0.5)) AS idf_score" + 
		"      FROM features f," + 
		"            doc_freq," + 
		"            docs_stats" + 
		"      WHERE f.term = doc_freq.term " + 
		"  )," + 
		"  term_scores AS (" + 
		"      SELECT" + 
		"             f.term," + 
		"             d.docid," + 
		"             (  " + 
		"              idf_scores_okapi.idf_score * (" + 
		"                              				 f.term_frequency * ($1 + 1) / (" + 
		"                                                                       			f.term_frequency + ( $1 * (" + 
		"                                                                                                      			1-$2+($2 * d.num_of_terms/docs_stats.avg_num_of_terms)" + 
		"                                                                                                          	)" + 
		"                                                                                          			) " + 
		"                                                                      			 )" + 
		"                              				)" + 
		"              ) AS score_okapi," + 
		"              (" + 
		"				  idf_scores_tfidf.idf_score * (1.0 + LOG(f.term_frequency))" + 
		"			  ) AS score_tf_idf" + 
		"      FROM" + 
		"          features f," + 
		"          documents d," + 
		"          idf_scores_tfidf," + 
		"          idf_scores_okapi," + 
		"          docs_stats" + 
		"      WHERE f.term = idf_scores_tfidf.term" + 
		"	  		AND f.term = idf_scores_okapi.term" + 
		"            AND f.docid = d.docid" + 
		"    )" + 
		"UPDATE features f " + 
		"SET score_tfidf = term_scores.score_tf_idf, " + 
		"    score_okapi = term_scores.score_okapi, " +
		"	 idf_tfidf = idf_scores_tfidf.idf_score, "+
		"	 idf_okapi = idf_scores_okapi.idf_score,"+
		"	 df = doc_freq.doc_count_of_term " +
		"FROM term_scores, idf_scores_okapi, idf_scores_tfidf, doc_freq " + 
		"WHERE f.term = term_scores.term AND f.docid = term_scores.docid AND f.term=idf_scores_okapi.term AND f.term=idf_scores_tfidf.term AND f.term=doc_freq.term; " +
		" "+
		"END; $$;";
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