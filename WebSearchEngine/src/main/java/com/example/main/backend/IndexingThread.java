package com.example.main.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.main.backend.api.SearchAPI;

@Component
public class IndexingThread extends Thread{

	@Value("${indexing.interval.milliseconds}")
	private int sleepSeconds;
	@Autowired
	SearchAPI api;

	/**
	 *
	 * @param seconds time to sleep till the next computation of tf idf scores
	 */
	public IndexingThread() {
		this.setDaemon(true);
	}

	@Override
	public void run() {
		try {
			while (true) {
				System.out.println("updating scores");
				api.updateScores();
				IndexingThread.sleep(sleepSeconds);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
