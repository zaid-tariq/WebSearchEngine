package com.example.main.backend;

import java.sql.*;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.example.main.backend.config.DBConfig;

@Component
@PropertySource(value = { "classpath:application.properties" }, ignoreResourceNotFound = false)
public class DatabaseCreator {

	private Connection connection;
	String url;
	String user;
	String pass;

	public DatabaseCreator() {
		DBConfig conf = new DBConfig();
		this.url = conf.getUrl();
		this.user = conf.getUsername();
		this.pass = conf.getPassword();
	}

	public DatabaseCreator(String url, String user, String pass) {
		this.url = url;
		this.user = user;
		this.pass = pass;
	}

	public Connection getConnection() throws SQLException {

		if (this.connection != null) {
			return this.connection;
		}
		this.connection = DriverManager.getConnection(url, user, pass);
		return connection;
	}

	public void create() {
		try {
			connection = this.getConnection();

			// CREATE TABLE IF NOT EXISTS s by DDL statements
			createDocumentsTable(connection);
			createFeatureTable(connection);
			createLinksTable(connection);
			createProcedureToTfIdfUpdate(connection);
			createFunctionConjunctive_search(connection);
			createFunctionDisjunctive_search(connection);
			createStatsFunction(connection);
			createIndices(connection);

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

	private void createFeatureTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS features (docid INT REFERENCES documents(docid), term TEXT, term_frequency INT, tf_idf FLOAT)");
		statement.execute();
		statement.close();
	}

	private void createDocumentsTable(Connection con) throws SQLException {
		PreparedStatement statement = con.prepareStatement(
				"CREATE TABLE IF NOT EXISTS documents (docid SERIAL PRIMARY KEY, url TEXT NOT NULL UNIQUE , crawled_on_date DATE, language TEXT)");
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
				"CREATE TABLE IF NOT EXISTS crawlerState (maximum_depth INT NOT NULL, maximum_docs INT NOT NULL, crawled_docs INT NOT NULL, leave_domain BOOLEAN, parallelism INT NOT NULL)");
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
	}

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
}