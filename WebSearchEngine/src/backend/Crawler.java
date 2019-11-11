package backend;

import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Crawler {

	private Queue<URL> urls = new LinkedList<>();
	private int maximumDepth = -1;
	private int maximumNumberOfDocs = -1;
	private boolean leaveDomain = false;
	private int parallelism = -1;

	public Thread crawlerThread;

	public Crawler(Set<URL> urls, int maximumDepth, int maximumNumberOfDocs, boolean leaveDomain, int parallelism) {
		this.urls.addAll(urls);
		this.maximumDepth = maximumDepth;
		this.maximumNumberOfDocs = maximumNumberOfDocs;
		this.leaveDomain = leaveDomain;
		this.parallelism = parallelism;
		crawlerThread = new Thread() {
			public void run() {
				// TODO: crawl logic
			}
		};
	}

	public void start() {

	}

	public void stop() {

	}
}
