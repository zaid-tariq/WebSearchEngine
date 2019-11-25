package com.example.main.backend;

import com.example.main.backend.api.SearchAPI;

public class IndexingThread extends Thread {

	private int sleepSeconds = 600;

	/**
	 *
	 * @param seconds time to sleep till the next computation of tf idf scores
	 */
	public IndexingThread(int seconds) {
		this.setDaemon(true);
		this.sleepSeconds = seconds;
	}

	@Override
	public void run() {
		try {
			while (true) {
				SearchAPI searchAPI = new SearchAPI();
				searchAPI.updateScores();
				IndexingThread.sleep(sleepSeconds);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
