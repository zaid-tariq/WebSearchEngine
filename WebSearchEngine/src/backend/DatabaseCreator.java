package backend;

import java.sql.*;
import java.util.Properties;

public class DatabaseCreator {

	private Connection connection;

	public DatabaseCreator() {
		Properties props = new Properties();

	}

	public Connection getConnection() throws SQLException {
		
		if(this.connection != null) {
			return this.connection;
		}

		String url = "jdbc:postgresql://localhost:5432/project";
		Properties props = new Properties();
		props.setProperty("user", "app");
		props.setProperty("password", "pass");
		this.connection = DriverManager.getConnection(url, props);
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
				"CREATE TABLE IF NOT EXISTS documents (docid SERIAL PRIMARY KEY, url TEXT NOT NULL , crawled_on_date DATE, language TEXT)");
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
				"CREATE TABLE IF NOT EXISTS crawlerState (id INT PRIMARY KEY, maximum_depth INT NOT NULL, maximum_docs INT NOT NULL, crawled_docs INT NOT NULL, leave_domain BOOLEAN, parallelism INT NOT NULL)");
		statement.execute();
		statement.close();
	}

	private void createProcedureToTfIdfUpdate(Connection con) throws SQLException {
		String query = "CREATE OR REPLACE PROCEDURE update_tf_idf_scores() " + 
				" LANGUAGE 'sql' " + 
				" AS $procedure$	CREATE TABLE features_temp AS" + 
				"		WITH " + 
				"			doc_freq AS (" + 
				"				SELECT term, COUNT(DISTINCT docid) as doc_count_of_term" + 
				"				FROM features" + 
				"				GROUP BY term" + 
				"			)," + 
				"			total_docs AS (" + 
				"				SELECT COUNT( DISTINCT docid) as num" + 
				"				FROM features" + 
				"			)," + 
				"			idf_scores AS (" + 
				"				SELECT f.term, LOG( 1.0 * total_docs.num / doc_freq.doc_count_of_term) as idf_score" + 
				"				FROM features f, doc_freq, total_docs			" + 
				"				WHERE f.term = doc_freq.term" + 
				"			)" + 
				"		SELECt f.docid, f.term, f.term_frequency, (i.idf_score * (1.0 + LOG(f.term_frequency) )) AS tf_idf" + 
				"		FROM idf_scores i, features f" + 
				"		WHERE i.term = f.term;" + 
				"		" + 
				"	DROP TABLE features;" + 
				"	ALTER TABLE features_temp" + 
				"		RENAME TO features;" + 
				"" + 
				"	$procedure$";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void createFunctionConjunctive_search(Connection con) throws SQLException {
		String query = "DROP FUNCTION  IF EXISTS conjunctive_search(text,integer); "
				+ "		CREATE OR REPLACE FUNCTION public.conjunctive_search(IN search_terms text,IN top_k integer)"
				+ "    RETURNS TABLE(doc_id integer, tf_idf_score double precision)" 
				+ "    LANGUAGE 'plpgsql'"
				+ "		AS $BODY$ BEGIN"
				+ "		DROP TABLE IF EXISTS search_terms_table;" 
				+ "		CREATE TEMP TABLE search_terms_table(term text);"
				+ "		INSERT INTO search_terms_table SELECT unnest(string_to_array(search_terms, ' '));"
				+ "		RETURN QUERY" 
				+ "				WITH " 
				+ "					doc_content AS("
				+ "						SELECT DISTINCT f1.docid, ARRAY("
				+ "														SELECT term FROM features f2"
				+ "														WHERE f2.docid = f1.docid"
				+ "													) as content"
				+ "						FROM features f1 ORDER BY docid" 
				+ "					)"
				+ "				SELECT f.docid, SUM(tf_idf) as tf_idf" 
				+ "				from features f, doc_content dc"
				+ "				WHERE " 
				+ "						f.docid=dc.docid"
				+ "						AND NOT EXISTS("
				+ "							SELECt * FROM search_terms_table EXCEPT"
				+ "							SELECT unnest(dc.content) )" 
				+ "				GROUP BY f.docid"
				+ "				ORDER BY tf_idf" 
				+ "				LIMIT top_k;" 
				+ "END; $BODY$;";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}

	private void createFunctionDisjunctive_search(Connection con) throws SQLException {
		String query = ""
				+ "	DROP FUNCTION  IF EXISTS disjunctive_search(text,integer); "
				+ "	CREATE OR REPLACE FUNCTION public.disjunctive_search(search_terms text, top_k integer)" + 
				" 	RETURNS TABLE(doc_id integer, tf_idf_score real)" + 
				" 	LANGUAGE plpgsql " + 
				"	AS $function$ BEGIN" + 
				"	DROP TABLE IF EXISTS search_terms_table;" + 
				"	CREATE TEMP TABLE search_terms_table(term text);" + 
				"	INSERT INTO search_terms_table SELECT DISTINCT unnest(string_to_array(search_terms, ' '));" + 
				"	RETURN QUERY" + 
				"				WITH " + 
				"					doc_content AS(" + 
				"						SELECT DISTINCT f1.docid, ARRAY(" + 
				"														SELECT term FROM features f2" + 
				"														WHERE f2.docid = f1.docid" + 
				"													) as content" + 
				"						FROM features f1 ORDER BY docid" + 
				"					)" + 
				"				SELECT f.docid, CAST( SUM(tf_idf) AS FLOAT(3)) as tf_idf" + 
				"				from features f, doc_content dc, search_terms_table st" + 
				"				WHERE	f.docid=dc.docid" + 
				"						AND f.term = st.term" + 
				"						AND EXISTS(" + 
				"							SELECt * FROM search_terms_table INTERSECT" + 
				"							(SELECT unnest(dc.content)) )" + 
				"" + 
				"				GROUP BY f.docid" + 
				"				ORDER BY tf_idf" + 
				"				LIMIT top_k;" + 
				"END; $function$" + 
				"";
		Statement statement = con.createStatement();
		statement.execute(query);
		statement.close();
	}
}