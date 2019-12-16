package com.example.main.backend.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import com.example.main.backend.HTMLDocument;
import com.example.main.backend.HTMLDocument.Language;

/**
 * Language detector based on the following article proposed on the sheet and
 * the provided resources: <a href=
 * "http://practicalcryptography.com/miscellaneous/machine-learning/tutorial-automatic-language-identification-word-ba/">http://practicalcryptography.com/miscellaneous/machine-learning/tutorial-automatic-language-identification-word-ba/</a>
 *
 */
public class LanguageDetector {

	private HashMap<String, Integer> germanWordCounts;
	private HashMap<String, Integer> englishWordCounts;
	private int wordCountGerman = 0;
	private int wordCountEnglish = 0;

	public LanguageDetector() {
		try {
			germanWordCounts = loadWordCounts(Language.GERMAN);
			englishWordCounts = loadWordCounts(Language.ENGLISH);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : germanWordCounts.keySet()) {
			wordCountGerman += germanWordCounts.get(key);
		}

		for (String key : englishWordCounts.keySet()) {
			wordCountEnglish += englishWordCounts.get(key);
		}
	}

	/**
	 * As said in the task if it is classified as English document we return that
	 * otherwise we German
	 * 
	 * @param words Words to classify over
	 * @return {@link HTMLDocument.Language} constant
	 */
	public String detect(List<String> words) {
		double probabilityGerman = 1;
		double probabilityEnglish = 1;

		for (String w : words) {
			probabilityGerman *= getProbability(Language.GERMAN, w.toLowerCase());
			probabilityEnglish *= getProbability(Language.ENGLISH, w.toLowerCase());
		}

		System.out.println("Probabilities: "+probabilityGerman+ " "+probabilityEnglish);
		
		return probabilityEnglish > probabilityGerman ? Language.ENGLISH : Language.GERMAN;
	}

	/**
	 * Loads the word counts for any supported language from a text file
	 * 
	 * @param lang Language constant provided by {@link HTMLDocument.Language}
	 * @return Word counts
	 * @throws IOException 
	 */
	private HashMap<String, Integer> loadWordCounts(String lang) throws IOException {
		if (lang.equals(Language.ENGLISH)) {
			HashMap<String, Integer> m = new HashMap<String, Integer>();
			File file = null;
			try {
				file = new ClassPathResource("english_counts.txt").getFile();
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			}
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line;
			while ((line = r.readLine()) != null) {
				String[] split = line.split(" ");
				m.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
			}
			r.close();
			return m;
		} else if (lang.equals(Language.GERMAN)) {
			HashMap<String, Integer> m = new HashMap<String, Integer>();
			File file = null;
			try {
				file = new ClassPathResource("german_counts.txt").getFile();
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			}
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line;
			while ((line = r.readLine()) != null) {
				String[] split = line.split(" ");
				m.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
			}
			r.close();
			return m;
		} else {
			throw new IllegalArgumentException("Language not supported");
		}
	}
	
	/**
	 * Returns the probability for that specific term
	 * 
	 * @param term
	 * @return word count/words in corpus if term exists in corpus otherwise 1/words
	 *         in corpus
	 */
	private double getProbability(String language, String term) {
		if (Language.GERMAN.equals(language)) {
			int frequency = germanWordCounts.getOrDefault(term, 1);
			return Math.log(((double)frequency) / wordCountGerman);
		} else if (Language.ENGLISH.equals(language)) {
			int frequency = englishWordCounts.getOrDefault(term, 1);
			return Math.log(((double)frequency) / wordCountEnglish);
		} else {
			throw new IllegalArgumentException("Language not supported");
		}
	}
}