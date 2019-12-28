package com.example.main;

import java.net.URISyntaxException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.main.backend.DatabaseCreator;
import com.example.main.backend.IndexingThread;

@Profile("indexer")
@Service
public class CLIIndexing implements CommandLineRunner {
	
	@Autowired
	IndexingThread indexer;
	
	@Autowired
	DatabaseCreator dbc;
	

	public void run(String... args) throws SQLException, URISyntaxException {
		dbc.create();
		indexer.start();

	}
}
