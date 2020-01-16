package com.example.main.backend;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class IndexingThread extends Thread {
	
	@Value("${indexing.interval.milliseconds}")	
	private int sleepSeconds;
	
	@Autowired
	DBHandler db;

	public IndexingThread() {
		this.setDaemon(true);
	}

	@Override
	public void run() {
		try {
			while (true) {
				try {
					//probably should have all these in separate individual threads
					db.updateDocFrequencies();
					db.updateStats();
					db.updateIdfScores();
					db.computePageRank(0.1,0.001);
					db.updateScores();
				} catch (SQLException ex) {
					ex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				IndexingThread.sleep(sleepSeconds);
				System.out.println("Finished");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
