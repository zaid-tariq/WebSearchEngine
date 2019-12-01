package com.example.main;

import java.net.URISyntaxException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.example.main.backend.IndexingThread;

@Service
public class CLIIndexing implements CommandLineRunner{
	
	@Autowired
	IndexingThread indexer;

	public void run(String... args) throws SQLException, URISyntaxException {
		indexer.start();
	}
}
