package com.example.main.backend.utils;

import java.util.TreeMap;

/**
 * NOTICE: The implementation of that class is based on the following paper:
 * https://cs.pomona.edu/~dkauchak/ir_project/whitepapers/Snippet-IL.pdf
 *
 */
public class SnippetGenerator {

	private int k;
	private TreeMap<String, Integer> occurrences;
	private String[] content;
	private DPEntry[][] dpTable;

	public SnippetGenerator(String[] content, TreeMap<String, Integer> occurrences, int k) {
		this.k = k;
		this.occurrences = occurrences;
		this.content = content;
		this.dpTable = new DPEntry[content.length][content.length];
	}

	public Object[] generateSnippet() {
		dynamicProgramming(0, content.length - 1);

		String s = "";
		for (int x = Math.max(0,
				dpTable[0][content.length - 1].start - 4); x < dpTable[0][content.length - 1].end; x++) {
			if (s.equals("")) {
				s += content[x];
			} else {
				s += " " + content[x];
			}
		}

		return new Object[] { s, dpTable[0][content.length - 1].score };
	}

	private double dynamicProgramming(int i, int j) {
		if (this.dpTable[i][j] != null) {
			return this.dpTable[i][j].score;
		}

		if (j - i <= 4) {
			System.out.println("Call: " + i + " " + j);
			this.dpTable[i][j] = new DPEntry(i, j, getScoreForTermAt(i));
			return getScoreForTermAt(i);
		} else if (j - i > k) {
			double firstArg = dynamicProgramming(i + 1, j);
			double secondArg = dynamicProgramming(i, j - 1);
			double max = Math.max(firstArg, secondArg);

			if (firstArg == max) {
				this.dpTable[i][j] = this.dpTable[i + 1][j];
			} else {
				this.dpTable[i][j] = this.dpTable[i][j - 1];
			}
			return max;
		} else {
			double firstArg = dynamicProgramming(i + 1, j);
			double secondArg = dynamicProgramming(i, j - 1);
			double thirdArg = getScoreForTermsInRange(i, j);
			double max = Math.max(firstArg, Math.max(secondArg, thirdArg));

			if (firstArg == max) {
				this.dpTable[i][j] = this.dpTable[i + 1][j];
			} else if (secondArg == max) {
				this.dpTable[i][j] = this.dpTable[i][j - 1];
			} else {
				this.dpTable[i][j] = new DPEntry(i, j, max);
			}
			return max;
		}
	}

	private double getScoreForTermAt(int position) {
		return occurrences.getOrDefault(content[position], -1);
	}

	private double getScoreForTermsInRange(int i, int j) {
		double score = 0;
		for (int x = i; x <= j; x++) {
			score += getScoreForTermAt(x);
		}
		return score;
	}

	private class DPEntry {

		double score;
		int start;
		int end;

		public DPEntry(int start, int end, double score) {
			this.start = start;
			this.end = end;
			this.score = score;
		}
	}
}